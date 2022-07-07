package org.jh.tools.daml;

public class UpgraderClient
{
    public static void main(String[] args)
    {
        System.out.println("Welcome to the DAML smart contract upgrade contract generator.\n");
        System.out.println("Given two DAR files, the upgrader will generate upgrade contracts for cases where upgrades can be automated\n");
        if (args.length < 3 || args.length > 4)
        {
            System.out.println("Usage: <path_to_dar_upgrade_from> <path_to_dar_to_upgrade_to> <output_directory> [<data_dependencies>]");
        }
        else
        {
            String filePath1 = args[0];
            String filePath2 = args[1];
            String outputDirectory = args[2];

            if (args.length == 4)
            {
                String dataDependencies = args[3];
                Upgrader.createUpgrades(filePath1, filePath2, outputDirectory, dataDependencies);
            }
            else
            {
                Upgrader.createUpgrades(filePath1, filePath2, outputDirectory, null);
            }
        }
    }
}
