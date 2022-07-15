package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf1;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FieldsDiffsDifferent extends FieldsDiffs
{
    private final Set<String> newFields = new LinkedHashSet<>();
    private final Set<String> removedFields = new LinkedHashSet<>();
    private final Set<String> fieldsInBoth = new LinkedHashSet<>();

    private final FieldsIndex fieldsIndexTo;

    FieldsDiffsDifferent(FieldsIndex fieldsIndexFrom, FieldsIndex fieldsIndexTo)
    {
        super(fieldsIndexFrom);
        this.fieldsIndexTo = fieldsIndexTo;
    }

    public static FieldsDiffsDifferent create(DamlLf1.DefDataType.Fields fieldsFrom, DamlLf1.Package _package1,
                                     DamlLf1.DefDataType.Fields fieldsTo, DamlLf1.Package _package2)
    {

        FieldsIndex fieldsIndexFrom = FieldsIndex.create(fieldsFrom, _package1);
        FieldsIndex fieldsIndexTo = FieldsIndex.create(fieldsTo, _package2);

        FieldsDiffsDifferent diffs = new FieldsDiffsDifferent(fieldsIndexFrom, fieldsIndexTo);

        for(String field : fieldsIndexFrom.fields.keySet())
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
            if(fieldsIndexFrom.fields.get(field) == null)
            {
                diffs.newFields.add(field);
            }
        }
        return diffs;
    }

    boolean fieldsHaveSameTypeAndAnyAdditionalFieldsAreOptional(Map<String, Map<String, FieldsDiffs>> dataTypes)
    {
        return this.fieldsInBothHaveSameType(dataTypes) &&
                //No new fields or all optional fields
                (this.newFields().isEmpty()
                        || this.newFields().stream().allMatch(s -> fieldsIndexTo.fields.get(s).isOptional()));
    }

    boolean fieldsInBothHaveSameType(Map<String, Map<String, FieldsDiffs>> dataTypes)
    {

        for(String fieldName : this.fieldsInBoth())
        {
            FieldsIndex.Type type1 = this.fieldsIndexFrom.fields.get(fieldName);
            FieldsIndex.Type type2 = this.fieldsIndexTo.fields.get(fieldName);

            if(type1.getClass() != type2.getClass())
            {
                return false;
            }

            if(type1 instanceof FieldsIndex.DataTypeRef)
            {
                FieldsIndex.DataTypeRef dataTypeRef1 = ((FieldsIndex.DataTypeRef) type1);
                FieldsIndex.DataTypeRef dataTypeRef2 = ((FieldsIndex.DataTypeRef) type2);

                if(!dataTypeRef1.moduleName.equals(dataTypeRef2.moduleName) ||
                        !dataTypeRef1.name.equals(dataTypeRef2.name))
                {
                    return false;
                }

                Map<String, FieldsDiffs> moduleTypes = dataTypes.get(dataTypeRef1.moduleName);
                if(moduleTypes != null)
                {
                    FieldsDiffs fieldsDiffs = moduleTypes.get(dataTypeRef1.name);
                    if(fieldsDiffs != null)
                        return fieldsDiffs.fieldsInBothHaveSameType(dataTypes);
                }
                return false;
            }

            if(!type1.toString().equals(type2.toString()))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    Iterable<String> getAdditionalOptionalFields()
    {
        return this.newFields().stream().filter(s -> fieldsIndexTo.fields.get(s).isOptional()).collect(Collectors.toList());
    }

    @Override
    Set<String> fieldsInBoth()
    {
        return this.fieldsInBoth;
    }

    @Override
    Set<String> newFields()
    {
        return this.newFields;
    }
}
