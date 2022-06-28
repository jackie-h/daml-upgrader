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

    private final String name;
    private final Map<String,String> sources;
    private final Map<String,DamlLf.Archive> damlLfArchivesByHash;

    public Dar(String name, Map<String, String> sources, Map<String,DamlLf.Archive> damlLfArchivesByHash)
    {
        this.name = name;
        this.sources = sources;
        this.damlLfArchivesByHash = damlLfArchivesByHash;
    }

    public static Dar readDar(String filePath)
    {
        Path path = Paths.get(filePath);
        String darName = path.getFileName().toString();
        String darNameWithoutExtension = darName.replace(".dar", "");
        DamlLf.Archive archiveProto = null;
        Map<String,String> sources = new HashMap<>();
        Map<String,DamlLf.Archive> archives = new HashMap<>();

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
                    archives.put(archiveProto.getHash(), archiveProto);
                    LOGGER.info(archiveProto.getHash());
                }
                else if (itemName.endsWith(".daml"))
                {
                    String file = new String(is.readAllBytes());
                    sources.put(name, file);
                }
            }

            return new Dar(darName, sources, archives);
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

    public Map<String,DamlLf.Archive> getDamlLfArchivesByHash()
    {
        return this.damlLfArchivesByHash;
    }

    //Most Dar files have a singular
    public DamlLf.Archive getDamlLf()
    {
        if (this.damlLfArchivesByHash.keySet().size() != 1)
        {
            throw new RuntimeException("The number of archives is not 1");
        }
        else
        {
            return this.damlLfArchivesByHash.values().iterator().next();
        }
    }
}
