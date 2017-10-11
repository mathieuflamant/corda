package net.corda.nodeapi.internal.serialization.amqp

import java.util.*
import net.corda.core.serialization.CordaSerializationTransformEnumDefaults
import net.corda.core.serialization.CordaSerializationTransformEnumDefault
import net.corda.core.serialization.CordaSerializationTransformRenames
import net.corda.core.serialization.CordaSerializationTransformRename
import org.apache.qpid.proton.amqp.DescribedType

// TODO:  it would be awesome to auto build this list by scanning for transform annotations themselves
// TODO: annotated with some annotation
/**
 *
 */
enum class TransformTypes(val f: (Annotation) -> Transform) {
    EnumDefault({ a -> EnumDefaultSchemeTransform((a as CordaSerializationTransformEnumDefault).old, a.new) }),
    Rename({ a -> RenameSchemaTransform((a as CordaSerializationTransformRename).from, a.to) })
}

// NOTE: We are effectively going to replicate the annotations, we need to do this because
// we can't instantiate instances of those annotation classes and this code needs to
// work at the de-serialising end
/**
 * Represents a specific type of transform, could be one or more instances of it
 */
sealed class Transform : DescribedType {
    companion object {
        val DESCRIPTOR = AMQPDescriptorRegistry.TRANSFORM_ELEMENT.amqpDescriptor
    }
    override fun getDescriptor(): Any = DESCRIPTOR

    override fun getDescribed(): Any = types
}

class EnumDefaultSchemeTransform(val old: String, val new: String) : Transform() {
    @Suppress("UNUSED")
    constructor (annotation: CordaSerializationTransformEnumDefault) : this(annotation.old, annotation.new)

}

class RenameSchemaTransform(val from: String, val to: String) : Transform() {
    @Suppress("UNUSED")
    constructor (annotation: CordaSerializationTransformRename) : this(annotation.from, annotation.to)
}

/**
 *
 */
private data class SupportedTransform(val type: Class<out Annotation>, val enum: TransformTypes)

/**
 * Utility list of all transforms we support that simplifies our generator
 */
private val supportedTransforms = listOf(
        SupportedTransform(CordaSerializationTransformEnumDefaults::class.java, TransformTypes.EnumDefault),
        SupportedTransform(CordaSerializationTransformRenames::class.java, TransformTypes.Rename)
)

/**
 * @property types is a list of serialised types that have transforms, each list element is a
 */
data class TransformsSchema(val types: Map<String, EnumMap<TransformTypes, MutableList<Transform>>>) : DescribedType {
    companion object {
        val DESCRIPTOR = AMQPDescriptorRegistry.TRANSFORM_SCHEMA.amqpDescriptor

        /**
         * @param schema should be a [Schema] generated for a serialised data structure
         * @param sf should be provided by the same sserialisationcontext that generated the schema
         */
        fun build(schema: Schema, sf: SerializerFactory): TransformsSchema {
            val rtn = mutableMapOf<String, EnumMap<TransformTypes, MutableList<Transform>>>()

            schema.types.forEach { type ->
                val clazz = sf.classloader.loadClass(type.name)

                supportedTransforms.forEach { transform ->
                    clazz.getAnnotation(transform.type)?.let { list ->
                        @Suppress("UNCHECKED_CAST")
                        (list::class.java.getDeclaredMethod("value").invoke(list) as Array<Annotation>).forEach {
                            val m = rtn.computeIfAbsent(type.name) {
                                EnumMap<TransformTypes, MutableList<Transform>>(TransformTypes::class.java)
                            }
                            m.computeIfAbsent(transform.enum) { mutableListOf() }.add(transform.enum.f(it))
                        }
                    }
                }
            }

            return TransformsSchema(rtn)
        }
    }
    override fun getDescriptor(): Any = DESCRIPTOR

    override fun getDescribed(): Any = types
}

