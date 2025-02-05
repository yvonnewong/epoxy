package com.airbnb.epoxy.processor

import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.isInt
import androidx.room.compiler.processing.isLong
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.WildcardTypeName

/**
 * This helps to memoize the look up of a type's information.
 */
class Type(val xType: XType, memoizer: Memoizer) {
    val typeName: TypeName by lazy {
        xType.typeNameWithWorkaround(memoizer)
    }
    val typeEnum: TypeEnum by lazy { TypeEnum.from(xType, typeName, memoizer) }

    enum class TypeEnum {
        StringOrCharSequence,
        Boolean,
        Int,
        Long,
        Double,
        ViewClickListener,
        ViewLongClickListener,
        ViewCheckedChangeListener,
        StringList,
        EpoxyModelList,
        StringAttributeData,
        Unknown;

        companion object {
            fun from(xType: XType, typeName: TypeName, memoizer: Memoizer): TypeEnum {

                val nonNullType by lazy { xType.makeNonNullable() }

                return when {
                    xType.isInt() -> Int
                    xType.isLong() -> Long
                    xType.typeName == TypeName.BOOLEAN || xType.typeName == TypeName.BOOLEAN.box() -> Boolean
                    xType.typeName == TypeName.DOUBLE || xType.typeName == TypeName.DOUBLE.box() -> Double
                    xType.isTypeOf(CharSequence::class) || xType.isTypeOf(String::class) -> StringOrCharSequence
                    xType.typeName == ClassNames.EPOXY_STRING_ATTRIBUTE_DATA -> StringAttributeData
                    // We don't care about nullability for the purposes of type checking
                    nonNullType == memoizer.viewOnClickListenerType -> ViewClickListener
                    nonNullType == memoizer.viewOnLongClickListenerType -> ViewLongClickListener
                    nonNullType == memoizer.viewOnCheckChangedType -> ViewCheckedChangeListener
                    xType.isTypeOf(List::class) -> {
                        val listType = xType.typeArguments.singleOrNull()
                        when {
                            listType?.isTypeOf(String::class) == true -> StringList
                            typeName == listOfGenericEpoxyModelType -> EpoxyModelList
                            else -> Unknown
                        }
                    }
                    else -> Unknown
                }
            }
        }
    }
}

private val listOfGenericEpoxyModelType = ParameterizedTypeName.get(
    ClassNames.LIST,
    WildcardTypeName.subtypeOf(
        ParameterizedTypeName.get(
            ClassNames.EPOXY_MODEL_UNTYPED,
            WildcardTypeName.subtypeOf(TypeName.OBJECT)
        )
    )
)
