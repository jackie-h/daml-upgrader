package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf;

import java.util.Map;

public class Dar
{
    private String name;
    private Map<String,String> sources;
    private DamlLf.Archive damlLf;

    public Dar(String name, Map<String, String> sources, DamlLf.Archive damlLf)
    {
        this.name = name;
        this.sources = sources;
        this.damlLf = damlLf;
    }

    public String getName()
    {
        return name;
    }

    public Map<String, String> getSources()
    {
        return sources;
    }

    public DamlLf.Archive getDamlLf()
    {
        return damlLf;
    }
}
