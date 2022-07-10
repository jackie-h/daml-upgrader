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
    private List<String> fieldsInBoth = new ArrayList<>();

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

    public Iterable<String> getFieldNames()
    {
        return this.fieldsIndexTo.fields.keySet();
    }

    protected boolean fieldIsPartyType(String fieldName)
    {
        FieldsIndex.Type type = this.fieldsIndexFrom.fields.get(fieldName);
        return type.type.hasPrim() && type.type.getPrim().getPrim().getValueDescriptor().getName().equals("PARTY");
    }

    protected boolean hasUpgradableFields(DamlLf1.Package _package)
    {
        //todo - handle complex record types and generics are also not complex
        return this.fieldsIndexFrom.fields.values().stream().allMatch(type -> {
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

    protected boolean isSchemaUpgradable()
    {
        return this.fieldsInBothSameHaveSameType() && this.newFields.isEmpty();
    }

    private boolean fieldsInBothSameHaveSameType()
    {

        for(String fieldName : this.fieldsInBoth)
        {
            FieldsIndex.Type type1 = this.fieldsIndexFrom.fields.get(fieldName);
            FieldsIndex.Type type2 = this.fieldsIndexTo.fields.get(fieldName);
            if(!type1.typeAsString.equals(type2.typeAsString))
            {
                return false;
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
                Type type = getType(ft.getType(), _package);
                if(type.type.hasPrim())
                {
                    if(DamlLfProtoUtils.isOptional(type.type))
                    {
                        fieldsIndex.optionalFields.add(fieldName);
                    }

                    for(DamlLf1.Type argType : type.type.getPrim().getArgsList())
                    {
                        type.args.add(getType(argType, _package));
                    }
                }

                fieldsIndex.fields.put(fieldName, type);
            }
            return fieldsIndex;
        }

        private static Type getType(DamlLf1.Type lf_type, DamlLf1.Package _package)
        {
            Type type = new Type();
            type.type = DamlLfProtoUtils.resolveType(lf_type, _package);

            StringBuilder builder = new StringBuilder();
            DamlLfPrinter.print(builder, "", lf_type, _package);
            type.typeAsString = builder.toString();
            return type;
        }

        private static class Type
        {
            private DamlLf1.Type type;
            private String typeAsString;
            private List<Type> args = new ArrayList<>();
        }
    }

}
