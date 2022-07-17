package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf1;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FieldsDiffsSame extends FieldsDiffs
{
    private final Set<String> empty = new HashSet<>();

    FieldsDiffsSame(FieldsIndex fieldsIndexFrom)
    {
        super(fieldsIndexFrom);
    }

    @Override
    Set<String> fieldsInBoth()
    {
        return this.fieldsIndexFrom.fields.keySet();
    }

    @Override
    Set<String> newFields()
    {
        return empty;
    }

    @Override
    Iterable<String> getAdditionalOptionalFields()
    {
        return empty;
    }

    @Override
    boolean fieldsHaveSameTypeAndAnyAdditionalFieldsAreOptional(Map<String, Map<String, FieldsDiffs>> dataTypes)
    {
        return true;
    }

    @Override
    boolean fieldsInBothHaveSameType(Map<String, Map<String, FieldsDiffs>> dataTypes)
    {
        return true;
    }

    public static FieldsDiffsSame create(DamlLf1.DefDataType.Fields fieldsFrom, DamlLf1.Package _package1)
    {
        FieldsIndex fieldsIndexFrom = FieldsIndex.create(fieldsFrom, _package1);
        return new FieldsDiffsSame(fieldsIndexFrom);
    }
}
