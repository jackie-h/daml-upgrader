package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf1;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

abstract class FieldsDiffs
{
    FieldsIndex fieldsIndexFrom;

    FieldsDiffs(FieldsIndex fieldsIndexFrom)
    {
        this.fieldsIndexFrom = fieldsIndexFrom;
    }

    abstract Set<String> fieldsInBoth();

    abstract Set<String> newFields();

    abstract Iterable<String> getAdditionalOptionalFields();

    abstract boolean fieldsHaveSameTypeAndAnyAdditionalFieldsAreOptional(Map<String, Map<String, FieldsDiffs>> dataTypes);

    abstract boolean fieldsInBothHaveSameType(Map<String, Map<String, FieldsDiffs>> dataTypes);

    abstract boolean hasUpgradableFields(Map<String, Map<String, FieldsDiffs>> dataTypes);

    FieldConstructors getFieldsInBothCopyConstructor(Map<String, Map<String, FieldsDiffs>> dataTypes)
    {
        Set<String> imports = new HashSet<>();
        Iterable<String> fieldCopy = this.getFieldsInBothCopyConstructorRecursive(dataTypes, imports, "cert");
        return new FieldConstructors(imports, fieldCopy);
    }

    private Iterable<String> getFieldsInBothCopyConstructorRecursive(
            Map<String, Map<String, FieldsDiffs>> dataTypes, Set<String> imports, String prefix)
    {
        List<String> fieldCopy = new ArrayList<>();
        for(String fieldName: this.fieldsInBoth())
        {
            FieldsIndex.Type type = fieldsIndexFrom.fields.get(fieldName);

            if(type instanceof FieldsIndex.DataTypeRef)
            {
                FieldsIndex.DataTypeRef dataTypeRef = (FieldsIndex.DataTypeRef)type;
                FieldsDiffs diffs = dataTypes.get(dataTypeRef.moduleName).get(dataTypeRef.name);
                if(diffs instanceof FieldsDiffsDifferent)
                {
                    imports.add("V2." + dataTypeRef.moduleName);
                    fieldCopy.add(fieldName + " = " + dataTypeRef.name + " with " +
                            String.join("; ",diffs.getFieldsInBothCopyConstructorRecursive(dataTypes, imports, prefix + "." + fieldName)));
                }
                else
                {
                    //the data types are the same, so there is no need to do a copy constructor
                    fieldCopy.add(fieldName + " = " + prefix + "." + fieldName);
                }
            }
            else
            {
                fieldCopy.add(fieldName + " = " + prefix + "." + fieldName);
            }
        }
        return fieldCopy;
    }

    public boolean fieldIsPartyType(String fieldName)
    {
        FieldsIndex.Type type = this.fieldsIndexFrom.fields.get(fieldName);
        return type instanceof FieldsIndex.PrimitiveType && ((FieldsIndex.PrimitiveType)type).name.equals("PARTY");
    }

    static class FieldsIndex
    {
        final Map<String,Type> fields = new LinkedHashMap<>();

        static FieldsIndex create(DamlLf1.DefDataType.Fields fields, DamlLf1.Package _package)
        {
            FieldsIndex fieldsIndex = new FieldsIndex();
            for(DamlLf1.FieldWithType ft: fields.getFieldsList())
            {
                String fieldName = DamlLfProtoUtils.getFieldWithTypeFieldName(_package, ft);
                Type type = getType(ft.getType(), _package);
                fieldsIndex.fields.put(fieldName, type);
            }
            return fieldsIndex;
        }

        private static Type getType(DamlLf1.Type lf_type, DamlLf1.Package _package)
        {
            lf_type = DamlLfProtoUtils.resolveType(lf_type, _package);
            BaseType type = null;
            if(lf_type.hasPrim())
            {
                PrimitiveType primitiveType = new PrimitiveType();
                primitiveType.name = lf_type.getPrim().getPrim().getValueDescriptor().getName();
                type = primitiveType;

                for(DamlLf1.Type argType : lf_type.getPrim().getArgsList())
                {
                    type.args.add(getType(argType, _package));
                }
            }
            else if(lf_type.hasNat())
            {
                NatType nt = new NatType();
                nt.value = lf_type.getNat();
                type = nt;
            }
            else if(lf_type.hasCon())
            {
                DamlLf1.Type.Con tCon = lf_type.getCon();
                if(tCon.hasTycon())
                {
                    DamlLf1.TypeConName typeConName = tCon.getTycon();
                    DataTypeRef dtr = new DataTypeRef();
                    dtr.name = DamlLfProtoUtils.getName(_package, typeConName);

                    if(typeConName.hasModule())
                    {
                        DamlLf1.ModuleRef ref = typeConName.getModule();
                        dtr.moduleName = DamlLfProtoUtils.getModuleName(_package, ref);
                    }
                    type = dtr;
                }
            }

            if (type == null)
            {
                UnknownType ut = new UnknownType();
                ut.type = lf_type;
                StringBuilder builder = new StringBuilder();
                DamlLfPrinter.print(builder, "", lf_type, _package);
                ut.typeAsString = builder.toString();
                type = ut;
            }

            return type;
        }

        interface Type
        {
            boolean isOptional();

            boolean upgradableType(Map<String, Map<String, FieldsDiffs>> dataTypes);

        }

        private static class BaseType implements Type
        {
            private List<Type> args = new ArrayList<>();

            @Override
            public boolean isOptional()
            {
                return false;
            }

            @Override
            public boolean upgradableType(Map<String, Map<String, FieldsDiffs>> dataTypes)
            {
                return this.args.stream().allMatch(
                        t -> !(t instanceof DataTypeRef) && t.upgradableType(dataTypes));
            }

            public String toString()
            {
                return args.stream().map(String::valueOf).collect(Collectors.joining(",", "<", ">"));
            }
        }

        private static class PrimitiveType extends BaseType
        {
            String name;

            @Override
            public boolean isOptional()
            {
                return "OPTIONAL".equals(name);
            }

            public String toString()
            {
                return this.name + super.toString();
            }
        }

        private static class NatType extends BaseType
        {
            long value;

            public String toString()
            {
                return this.value + super.toString();
            }
        }

        static class DataTypeRef extends BaseType
        {
            String moduleName;
            String name;

            @Override
            public boolean upgradableType(Map<String, Map<String, FieldsDiffs>> dataTypes)
            {
                Map<String, FieldsDiffs> moduleDataTypes = dataTypes.get(moduleName);
                if(moduleDataTypes != null)
                {
                    FieldsDiffs diffs = moduleDataTypes.get(this.name);
                    if(diffs != null)
                    {
                        //todo: this won't work for circular refs - fix
                        return diffs.hasUpgradableFields(dataTypes);
                    }
                }

                return false;
            }

            public String toString()
            {
                return this.moduleName + "[" + this.name + "]";
            }
        }

        private static class UnknownType extends BaseType
        {
            private DamlLf1.Type type;
            private String typeAsString;

            @Override
            public boolean upgradableType(Map<String, Map<String, FieldsDiffs>> dataTypes)
            {
                return true;
            }

            public String toString()
            {
                return this.typeAsString + super.toString();
            }
        }
    }

}
