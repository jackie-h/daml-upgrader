package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Dar
{
    private static final Logger LOGGER =  Logger.getLogger(Dar.class.getName());

    private String name;
    private Map<String,String> sources;
    private DamlLf.Archive damlLf;

    public Dar(String name, Map<String, String> sources, DamlLf.Archive damlLf)
    {
        this.name = name;
        this.sources = sources;
        this.damlLf = damlLf;
    }

    public static Dar readDar(String filePath)
    {
        Path path = Paths.get(filePath);
        String darName = path.getFileName().toString();
        String darNameWithoutExtension = darName.replace(".dar", "");
        DamlLf.Archive archiveProto = null;
        Map<String,String> sources = new HashMap<>();

        try(ZipInputStream is = new ZipInputStream(java.nio.file.Files.newInputStream(path)))
        {

            ZipEntry entry;
            while((entry = is.getNextEntry()) != null)
            {
                String name = entry.getName();
                LOGGER.info(name);

                Path zipContentPath = Paths.get(name);
                String itemName = zipContentPath.getFileName().toString();

                //Read the DALF file for this code, be careful not to read the dependencies
                if (itemName.startsWith(darNameWithoutExtension) && itemName.endsWith(".dalf"))
                {
                    byte[] bytes = is.readAllBytes();
                    archiveProto = DamlLf.Archive.parseFrom(bytes);
                    LOGGER.info(archiveProto.getHash());
                }
                else if (itemName.endsWith(".daml"))
                {
                    String file = new String(is.readAllBytes());
                    sources.put(name, file);
                }
            }

            return new Dar(darName, sources, archiveProto);
        }
        catch (IOException e)
        {
            //todo: clean-up
            e.printStackTrace();
            throw new RuntimeException("Failed to read archive:" + filePath);
        }

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
