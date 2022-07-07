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
import java.util.stream.Collectors;

public class Upgrader
{
    private static final Logger LOGGER =  Logger.getLogger(Upgrader.class.getName());

    public static Map<String, List<Module>> createUpgrades(String archivePathFrom, String archivePathTo,
                                                           String outputPath, String dataDependencies)
    {
        LOGGER.info("Starting upgrade");
        LOGGER.info("Archive Path From=" + archivePathFrom);
        LOGGER.info("Archive Path To=" + archivePathTo);

        Dar darFrom = Dar.readDar(archivePathFrom);
        Dar darTo = Dar.readDar(archivePathTo);

        Map<String, Map<String, TemplateDetails>> upgradeTemplateNamesByModule = identifyTemplatesToUpgrade(darFrom.getDamlLf(), darTo.getDamlLf());
        Map<String, List<Module>> upgrades = createUpgradeTemplates(upgradeTemplateNamesByModule);
        writeUpgradesToFiles(upgrades, outputPath, darTo.getSdkVersion(), archivePathFrom, archivePathTo, dataDependencies);
        List<TemplateDetails> details = upgradeTemplateNamesByModule.values().stream().flatMap(stringTemplateDetailsMap -> stringTemplateDetailsMap.values().stream())
                .collect(Collectors.toList());
        LOGGER.info(String.format("Created upgrades for %d/%d contracts", details.stream().filter(TemplateDetails::canAutoUpgrade).count(), details.size()));
        return upgrades;
    }

    private static Map<String, Map<String, TemplateDetails>> identifyTemplatesToUpgrade(DamlLf.Archive archiveFrom,
                                                                                        DamlLf.Archive archiveTo)
    {
        LOGGER.info(archiveFrom.getHash());
        LOGGER.info(archiveTo.getHash());

        if (archiveFrom.getHash().equals(archiveTo.getHash()))
        {
            LOGGER.info("Contents identical nothing to do");
            return new HashMap<>();
        }

        ArchivePayload payloadCurrent = Reader.readArchive(archiveFrom).right().get();
        ArchivePayload payloadNew = Reader.readArchive(archiveTo).right().get();
        LOGGER.info(payloadCurrent.pkgId());
        LOGGER.info(payloadNew.pkgId());

        return DamlLfProtoUtils.findTemplatesThatAreInOneAndInTwo(payloadCurrent.proto(), payloadNew.proto());
    }

    private static Map<String, List<Module>> createUpgradeTemplates(Map<String, Map<String, TemplateDetails>> upgrades)
    {
        Map<String, List<Module>> upgradesByModule = new HashMap<>();
        for(String moduleName : upgrades.keySet())
        {
            Map<String, TemplateDetails> templates = upgrades.get(moduleName);
            List<TemplateDetails> upgradeable = templates.values().stream()
                    .filter(TemplateDetails::canAutoUpgrade)
                    .collect(Collectors.toList());

            List<Module> contracts = UpgradeTemplate.createUpgradeTemplatesContent(moduleName, upgradeable);

            templates.values().stream().filter(t -> !t.canAutoUpgrade()).forEach(t -> {
                LOGGER.warning("Unable to upgrade template " + t.name() + " has a different schema");
            });
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

        for(String moduleName : upgrades.keySet())
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
            String modulePath = moduleName.replace(".","/");
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
