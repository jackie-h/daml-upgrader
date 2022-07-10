package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf1;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FieldsDiffs
{
    private List<String> newFields = new ArrayList<>();
    private List<String> removedFields = new ArrayList<>();

    private FieldsIndex fieldsIndex1;
    private FieldsIndex fieldsIndex2;

    public static FieldsDiffs create(DamlLf1.DefDataType.Fields fields1, DamlLf1.Package _package1,
                                      DamlLf1.DefDataType.Fields fields2, DamlLf1.Package _package2)
    {
        FieldsDiffs diffs = new FieldsDiffs();
        diffs.fieldsIndex1 = FieldsIndex.create(fields1, _package1);
        diffs.fieldsIndex2 = FieldsIndex.create(fields2, _package2);

        for(String field : diffs.fieldsIndex1.fields.keySet())
        {
            if(diffs.fieldsIndex2.fields.get(field) == null)
            {
                diffs.newFields.add(field);
            }
        }
        for(String field : diffs.fieldsIndex2.fields.keySet())
        {
            if(diffs.fieldsIndex1.fields.get(field) == null)
            {
                diffs.removedFields.add(field);
            }
        }
        return diffs;
    }

    public Iterable<String> getFieldNames()
    {
        return this.fieldsIndex1.fields.keySet();
    }

    protected boolean fieldIsPartyType(String fieldName)
    {
        FieldsIndex.Type type = this.fieldsIndex1.fields.get(fieldName);
        return type.type.hasPrim() && type.type.getPrim().getPrim().getValueDescriptor().getName().equals("PARTY");
    }

    protected boolean hasUpgradableFields(DamlLf1.Package _package)
    {
        //todo - handle complex record types and generics are also not complex
        return this.fieldsIndex1.fields.values().stream().allMatch(type -> {
            if(type.type.hasPrim() && type.type.getPrim().getArgsCount() > 0)
            {
                for(DamlLf1.Type argType : type.type.getPrim().getArgsList())
                {
                    if(argType.hasInterned())
                    {
                        argType = _package.getInternedTypes(argType.getInterned());
                        if(!argType.hasPrim() && !argType.hasNat()) //decimal types have natural args
                            return false;
                    }
                }
            }
            return type.type.hasPrim();
        });
    }

    protected boolean isSchemaSame()
    {
        if(this.fieldsIndex1.fields.keySet().size() != this.fieldsIndex2.fields.keySet().size())
        {
            return false;
        }
        else
        {
            Map<String, FieldsIndex.Type> fieldsOne = this.fieldsIndex1.fields;
            Map<String, FieldsIndex.Type> fieldsTwo = this.fieldsIndex2.fields;

            for(String fieldName : fieldsOne.keySet())
            {
                FieldsIndex.Type type1 = fieldsOne.get(fieldName);
                FieldsIndex.Type type2 = fieldsTwo.get(fieldName);
                if(type2 == null)
                {
                    return false;
                }
                if(!type1.typeAsString.equals(type2.typeAsString))
                {
                    return false;
                }
            }
        }

        return true;
    }

    private static class FieldsIndex
    {
        private final Map<String,Type> fields = new LinkedHashMap<>();
        private final Set<String> optionalFields = new HashSet<>();

        private static FieldsIndex create(DamlLf1.DefDataType.Fields fields, DamlLf1.Package _package)
        {
            FieldsIndex fieldsIndex = new FieldsIndex();
            for(DamlLf1.FieldWithType ft: fields.getFieldsList())
            {
                String fieldName = _package.getInternedStrings(ft.getFieldInternedStr());
                Type type = new Type();
                type.type = DamlLfProtoUtils.resolveType(ft.getType(), _package);

                StringBuilder builder = new StringBuilder();
                DamlLfPrinter.print(builder, "", ft.getType(), _package);
                type.typeAsString = builder.toString();

                fieldsIndex.fields.put(fieldName, type);

                if(DamlLfProtoUtils.isOptional(ft.getType(), _package))
                {
                    fieldsIndex.optionalFields.add(fieldName);
                }
            }
            return fieldsIndex;
        }

        private static class Type
        {
            private DamlLf1.Type type;
            private String typeAsString;
            private List<DamlLf1.Type> args;
        }
    }

}
