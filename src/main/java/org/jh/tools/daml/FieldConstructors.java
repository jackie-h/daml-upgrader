package org.jh.tools.daml;


import java.util.Set;

public class FieldConstructors
{
    private final Set<String> imports;
    private final Iterable<String> fieldSetters;

    public FieldConstructors(Set<String> imports, Iterable<String> fieldSetters)
    {
        this.imports = imports;
        this.fieldSetters = fieldSetters;
    }

    public Set<String> getImports()
    {
        return imports;
    }

    public Iterable<String> getFieldSetters()
    {
        return fieldSetters;
    }
}
