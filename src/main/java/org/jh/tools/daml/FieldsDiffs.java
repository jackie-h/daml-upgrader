package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf1;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FieldsDiffs
{
    private final List<String> newFields = new ArrayList<>();
    private final List<String> removedFields = new ArrayList<>();
    private final List<String> fieldsInBoth = new ArrayList<>();

    private FieldsIndex fieldsIndexFrom;
    private FieldsIndex fieldsIndexTo;

    public static FieldsDiffs create(DamlLf1.DefDataType.Fields fieldsFrom, DamlLf1.Package _package1,
                                      DamlLf1.DefDataType.Fields fieldsTo, DamlLf1.Package _package2)
    {
        FieldsDiffs diffs = new FieldsDiffs();
        diffs.fieldsIndexFrom = FieldsIndex.create(fieldsFrom, _package1);
        diffs.fieldsIndexTo = FieldsIndex.create(fieldsTo, _package2);

        for(String field : diffs.fieldsIndexFrom.fields.keySet())
        {
            if(diffs.fieldsIndexTo.fields.get(field) == null)
            {
                diffs.removedFields.add(field);
            }
            else
            {
                diffs.fieldsInBoth.add(field);
            }
        }
        for(String field : diffs.fieldsIndexTo.fields.keySet())
        {
            if(diffs.fieldsIndexFrom.fields.get(field) == null)
            {
                diffs.newFields.add(field);
            }
        }
        return diffs;
    }

    public Iterable<String> getFieldNamesInBoth()
    {
        return this.fieldsInBoth;
    }

    public Iterable<String> getAdditionalOptionalFields()
    {
        return this.newFields.stream().filter(s -> fieldsIndexTo.fields.get(s).isOptional()).collect(Collectors.toList());
    }

    protected boolean fieldIsPartyType(String fieldName)
    {
        FieldsIndex.Type type = this.fieldsIndexFrom.fields.get(fieldName);
        return type instanceof FieldsIndex.PrimitiveType && ((FieldsIndex.PrimitiveType)type).name.equals("PARTY");
    }

    protected boolean hasUpgradableFields(DamlLf1.Package _package)
    {
        //todo - handle complex record types and generics are also not complex
        return this.fieldsIndexFrom.fields.values().stream().allMatch(type -> {
            if(type instanceof FieldsIndex.PrimitiveType)
            {
                FieldsIndex.BaseType baseType = (FieldsIndex.BaseType)type;
                if(baseType.args.size() > 0)
                {
                    for (FieldsIndex.Type argType : baseType.args)
                    {
                        if (!(argType instanceof FieldsIndex.PrimitiveType)
                                && !(argType instanceof FieldsIndex.NatType)) //decimal types have natural args
                                return false;
                    }
                }
                return true;
            }
            return false;
        });
    }

    protected boolean isSchemaUpgradable()
    {
        return this.fieldsInBothHaveSameType() &&
                //No new fields or all optional fields
                (this.newFields.isEmpty()
                        || this.newFields.stream().allMatch(s -> fieldsIndexTo.fields.get(s).isOptional()));
    }

    private boolean fieldsInBothHaveSameType()
    {

        for(String fieldName : this.fieldsInBoth)
        {
            FieldsIndex.Type type1 = this.fieldsIndexFrom.fields.get(fieldName);
            FieldsIndex.Type type2 = this.fieldsIndexTo.fields.get(fieldName);
            if(!type1.toString().equals(type2.toString()))
            {
                return false;
            }
        }
        return true;
    }

    private static class FieldsIndex
    {
        private final Map<String,Type> fields = new LinkedHashMap<>();

        private static FieldsIndex create(DamlLf1.DefDataType.Fields fields, DamlLf1.Package _package)
        {
            FieldsIndex fieldsIndex = new FieldsIndex();
            for(DamlLf1.FieldWithType ft: fields.getFieldsList())
            {
                String fieldName = _package.getInternedStrings(ft.getFieldInternedStr());
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
                    if(typeConName.hasModule())
                    {
                        DamlLf1.ModuleRef ref = typeConName.getModule();
                        if(ref.hasModuleNameInternedDname())
                        {
                            String moduleName = DamlLfProtoUtils.getName(_package, ref.getModuleNameInternedDname());
                            String name = DamlLfProtoUtils.getName(_package, typeConName.getNameInternedDname());
                            DataTypeRef dtr = new DataTypeRef();
                            dtr.moduleName = moduleName;
                            dtr.name = name;
                            type = dtr;
                        }
                    }
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

        private interface Type
        {
            boolean isOptional();

        }

        private static class BaseType implements Type
        {
            private List<Type> args = new ArrayList<>();

            @Override
            public boolean isOptional()
            {
                return false;
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

        private static class DataTypeRef extends BaseType
        {
            String moduleName;
            String name;

            public String toString()
            {
                return this.moduleName + "[" + this.name + "]";
            }
        }

        private static class UnknownType extends BaseType
        {
            private DamlLf1.Type type;
            private String typeAsString;

            public String toString()
            {
                return this.typeAsString + super.toString();
            }
        }
    }

}
