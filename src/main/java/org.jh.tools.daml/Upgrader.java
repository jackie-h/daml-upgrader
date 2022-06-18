package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf;
import com.daml.lf.archive.ArchivePayload;
import com.daml.lf.archive.Reader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class Upgrader
{
    private static final Logger LOGGER =  Logger.getLogger(Upgrader.class.getName());

    public static Map<String, List<Module>> createUpgrades(String currentArchivePath, String newArchivePath, String outputPath)
    {
        LOGGER.info("Starting upgrade");
        LOGGER.info("Current Archive Path=" + currentArchivePath);
        LOGGER.info("New Archive Path=" + newArchivePath);

        DamlLf.Archive archiveOld = Dar.readDar(currentArchivePath).getDamlLf();
        DamlLf.Archive archiveNew = Dar.readDar(newArchivePath).getDamlLf();

        Map<String, List<String>> upgradeTemplateNamesByModule = identifyTemplatesToUpgrade(archiveOld, archiveNew);
        Map<String, List<Module>> upgrades = createUpgradeTemplates(upgradeTemplateNamesByModule);
        writeUpgradesToFiles(upgrades, outputPath);
        return upgrades;
    }

    private static Map<String, List<String>> identifyTemplatesToUpgrade(DamlLf.Archive archiveCurrent,
                                                                        DamlLf.Archive archiveNew)
    {
        LOGGER.info(archiveCurrent.getHash());
        LOGGER.info(archiveNew.getHash());

        if (archiveCurrent.getHash().equals(archiveNew.getHash()))
        {
            LOGGER.info("Contents identical nothing to do");
            return new HashMap<>();
        }

        ArchivePayload payloadCurrent = Reader.readArchive(archiveCurrent).right().get();
        ArchivePayload payloadNew = Reader.readArchive(archiveNew).right().get();
        LOGGER.info(payloadCurrent.pkgId());
        LOGGER.info(payloadNew.pkgId());

        return DamlLfProtoUtils.findTemplatesThatAreInOneAndInTwo(payloadCurrent.proto(), payloadNew.proto());
    }

    private static Map<String, List<Module>> createUpgradeTemplates(Map<String,List<String>> upgrades)
    {
        Map<String, List<Module>> upgradesByModule = new HashMap<>();
        for(String moduleName : upgrades.keySet())
        {

            List<String> contractNames = upgrades.get(moduleName);
            List<Module> contracts = UpgradeTemplate.createUpgradeTemplatesContent(moduleName, contractNames);
            upgradesByModule.put(moduleName, contracts);
        }
        return upgradesByModule;
    }

    private static void writeUpgradesToFiles(Map<String, List<Module>> upgrades, String outpath)
    {
        for(String moduleName : upgrades.keySet())
        {
            List<Module> contracts = upgrades.get(moduleName);
            writeUpgradeToFiles(moduleName, contracts, outpath);
        }
    }

    private static void writeUpgradeToFiles(String moduleName, List<Module> modules, String outpath)
    {
        String directory = outpath + "/" + moduleName + "/";
        try
        {
            Files.createDirectories(Paths.get(directory));
            for (Module upgradeModule : modules)
            {
                String fileName = directory + upgradeModule.getName() + ".daml";
                Path filePath = Paths.get(fileName);
                Files.writeString(filePath, upgradeModule.getContents());
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException("Failed to create upgrade module:" + moduleName);
        }
    }

}
