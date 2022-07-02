package org.jh.tools.daml;

import java.util.ArrayList;
import java.util.List;

public class DamlDiffs
{
    private final List<TemplateDetails> inBothNoSchemaChange = new ArrayList<>();
    private final List<TemplateDetails> inBothSchemaChange = new ArrayList<>();

    public void addTemplateWithSchemaChange(TemplateDetails templateDetails)
    {
        this.inBothSchemaChange.add(templateDetails);
    }

    public void addTemplateWithoutSchemaChange(TemplateDetails templateDetails)
    {
        this.inBothNoSchemaChange.add(templateDetails);
    }

    public List<TemplateDetails> getInBothNoSchemaChange()
    {
        return inBothNoSchemaChange;
    }

    public List<TemplateDetails> getInBothSchemaChange()
    {
        return inBothSchemaChange;
    }
}
