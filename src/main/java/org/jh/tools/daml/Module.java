package org.jh.tools.daml;

public class Module
{
    private final String name;
    private final String contents;

    public Module(String name, String contents)
    {
        this.name = name;
        this.contents = contents;
    }

    public String getName()
    {
        return name;
    }

    public String getContents()
    {
        return contents;
    }
}
