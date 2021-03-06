package org.jh.tools.daml;

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
    private static final Logger LOGGER = Logger.getLogger(Upgrader.class.getName());

    public static ArchiveDiffs createUpgrades(String archivePathFrom, String archivePathTo,
                                              String outputPath, String dataDependencies)
    {
        LOGGER.info("Starting upgrade");
        LOGGER.info("Archive Path From=" + archivePathFrom);
        LOGGER.info("Archive Path To=" + archivePathTo);

        Dar darFrom = Dar.readDar(archivePathFrom);
        Dar darTo = Dar.readDar(archivePathTo);

        ArchiveDiffs archiveDiffs = ArchiveDiffs.create(darFrom, darTo);
        Map<String, List<Module>> upgrades = createUpgradeTemplates(archiveDiffs);
        writeUpgradesToFiles(upgrades, outputPath, darTo.getSdkVersion(), archivePathFrom, archivePathTo, dataDependencies);
        LOGGER.info(String.format("\nCreated upgrades for %d/%d contracts\n", archiveDiffs.upgradableTemplateCount(), archiveDiffs.templateCount())
                + archiveDiffs.report());
        return archiveDiffs;
    }

    private static Map<String, List<Module>> createUpgradeTemplates(ArchiveDiffs archiveDiffs)
    {
        Map<String, List<Module>> upgradesByModule = new HashMap<>();
        for (String moduleName : archiveDiffs.modules())
        {
            List<String> upgradeable = archiveDiffs.upgradableTemplates(moduleName);

            List<Module> contracts = UpgradeTemplate.createUpgradeTemplatesContent(moduleName, upgradeable, archiveDiffs);
            upgradesByModule.put(moduleName, contracts);
        }
        return upgradesByModule;
    }

    private static void writeUpgradesToFiles(Map<String, List<Module>> upgrades, String outpath,
                                             String sdkVersion, String archivePathFrom,
                                             String archivePathTo, String dataDependency)
    {
        String archiveNameFrom = getFileNameWithoutExtension(archivePathFrom);
        String archiveNameTo = getFileNameWithoutExtension(archivePathTo);
        //todo - make it an option to use direct paths
        Path relativeArchivePathFrom = Paths.get(outpath).relativize(Paths.get(archivePathFrom));
        Path relativeArchivePathTo = Paths.get(outpath).relativize(Paths.get(archivePathTo));
        String projectYaml = UpgradeTemplate.createProjectYaml(sdkVersion, archiveNameFrom, archiveNameTo,
                relativeArchivePathFrom.toString(), relativeArchivePathTo.toString(), dataDependency);

        try
        {
            Path outputPath = Paths.get(outpath);
            Files.createDirectories(outputPath);
            Files.writeString(outputPath.resolve("daml.yaml"), projectYaml);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to create output:", e);
        }

        for (String moduleName : upgrades.keySet())
        {
            List<Module> contracts = upgrades.get(moduleName);
            writeUpgradeToFiles(moduleName, contracts, outpath);
        }
    }

    private static String getFileNameWithoutExtension(String archivePath)
    {
        String archiveFileName = Paths.get(archivePath).getFileName().toString();
        return archiveFileName.substring(0, archiveFileName.lastIndexOf("."));
    }

    private static void writeUpgradeToFiles(String moduleName, List<Module> modules, String outpath)
    {
        try
        {
            String modulePath = moduleName.replace(".", "/");
            Path directory = Paths.get(outpath, "daml", modulePath);
            Files.createDirectories(Paths.get(outpath, "daml", modulePath));
            for (Module upgradeModule : modules)
            {
                Path filePath = directory.resolve(upgradeModule.getName() + ".daml");
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