package org.jh.tools.daml;

public enum UpgradeDecision
{
    YES("Ok!"),
    NO_MULTI_PARTY("Don't know how to upgrade contracts with >2 parties yet"),
    NO_SCHEMA_CHANGE("Template schema changed in a way that is not auto-upgradable"),
    NO_TEMPLATE_REMOVED("Template was no longer found and seems to have been removed"),
    NO_UNSUPPORTED_TYPES("Template has a type that is currently not supported");

    private final String message;

    UpgradeDecision(String message)
    {
        this.message = message;
    }

    public String getMessage()
    {
        return message;
    }
}
