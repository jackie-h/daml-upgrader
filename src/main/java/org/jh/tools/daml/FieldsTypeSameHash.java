package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf1;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Used when the Type is in a dependency and the hash is the same don't need a conversion
 */
public class FieldsTypeSameHash extends FieldsDiffs
{
    private final Set<String> empty = new HashSet<>();

    FieldsTypeSameHash(FieldsIndex fieldsIndexFrom)
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

    boolean hasUpgradableFields(Map<String, Map<String, FieldsDiffs>> dataTypes)
    {
        return true;
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

    public static FieldsTypeSameHash create(DamlLf1.DefDataType.Fields fieldsFrom, DamlLf1.Package _package1)
    {
        FieldsIndex fieldsIndexFrom = FieldsIndex.create(fieldsFrom, _package1);
        return new FieldsTypeSameHash(fieldsIndexFrom);
    }
}
