package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Dar
{
    private static final Logger LOGGER =  Logger.getLogger(Dar.class.getName());

    private final String name;
    private final Map<String,String> sources;
    private final DamlLf.Archive mainArchive;
    private final Map<String,DamlLf.Archive> damlLfDependenciesArchivesByHash;
    private final Manifest manifest;
    private final String conf;

    private Dar(String name, Manifest manifest, String conf,
               DamlLf.Archive mainArchive,
               Map<String, String> sources, Map<String,DamlLf.Archive> damlLfDependenciesArchivesByHash)
    {
        this.name = name;
        this.manifest = manifest;
        this.conf = conf;
        this.sources = sources;
        this.mainArchive = mainArchive;
        this.damlLfDependenciesArchivesByHash = damlLfDependenciesArchivesByHash;
    }

    public static Dar readDar(String filePath)
    {
        Path path = Paths.get(filePath);
        String darName = path.getFileName().toString();
        Map<String,String> sources = new HashMap<>();
        Map<String,DamlLf.Archive> archives = new HashMap<>();
        Manifest manifest = null;
        String conf = null;

        try(ZipInputStream is = new ZipInputStream(java.nio.file.Files.newInputStream(path)))
        {
            ZipEntry entry;
            while((entry = is.getNextEntry()) != null)
            {
                String name = entry.getName();
                LOGGER.fine(name);

                Path zipContentPath = Paths.get(name);
                String itemName = zipContentPath.getFileName().toString();

                if (itemName.endsWith(".dalf"))
                {
                    byte[] bytes = is.readAllBytes();
                    DamlLf.Archive archiveProto = DamlLf.Archive.parseFrom(bytes);
                    archives.put(itemName, archiveProto);
                    LOGGER.fine(archiveProto.getHash());
                }
                else if (itemName.endsWith(".conf"))
                {
                    conf = new String(is.readAllBytes());
                }
                else if (itemName.endsWith(".daml"))
                {
                    String file = new String(is.readAllBytes());
                    sources.put(name, file);
                }
                else if ("MANIFEST.MF".equals(itemName))
                {
                    manifest = new Manifest(is);
                }
            }

            String mainDalfPath = manifest.getMainAttributes().getValue("Main-Dalf");
            List<String> main = archives.keySet().stream().filter(mainDalfPath::endsWith).collect(Collectors.toList());
            if (main.size() != 1)
            {
                throw new RuntimeException("The number of main archives is not 1");
            }
            else
            {
                DamlLf.Archive mainArchive = archives.remove(main.get(0));
                return new Dar(darName, manifest, conf, mainArchive, sources, archives);
            }
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

    public String getSdkVersion()
    {
        return manifest.getMainAttributes().getValue("Sdk-Version");
    }

    public String getMainDalfPath()
    {
        return manifest.getMainAttributes().getValue("Main-Dalf");
    }

    public Map<String, String> getSources()
    {
        return sources;
    }

    public DamlLf.Archive getMainDamlLf()
    {
        return this.mainArchive;
    }

    public Map<String,DamlLf.Archive> getDependencyDamlLfs()
    {
        return this.damlLfDependenciesArchivesByHash;
    }
}
