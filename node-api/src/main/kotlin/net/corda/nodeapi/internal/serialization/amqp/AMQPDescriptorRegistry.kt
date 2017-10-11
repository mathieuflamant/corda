package net.corda.nodeapi.internal.serialization.amqp

import org.apache.qpid.proton.amqp.UnsignedLong

// TODO: get an assigned number as per AMQP spec
const val DESCRIPTOR_TOP_32BITS: Long = 0xc0da0000

enum class AMQPDescriptorRegistry(val id: Long) {

    ENVELOPE(1),
    SCHEMA(2),
    OBJECT_DESCRIPTOR(3),
    FIELD(4),
    COMPOSITE_TYPE(5),
    RESTRICTED_TYPE(6),
    CHOICE(7),
    REFERENCED_OBJECT(8),
    TRANSFORM_SCHEMA(9),
    TRANSFORM_ELEMENT(10)
    ;

    val amqpDescriptor = UnsignedLong(id or DESCRIPTOR_TOP_32BITS)
}
