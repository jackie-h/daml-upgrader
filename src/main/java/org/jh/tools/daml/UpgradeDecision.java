package org.jh.tools.daml;

public enum UpgradeDecision
{
    YES,
    NO_MULTI_PARTY,
    NO_SCHEMA_CHANGE,
    NO_TEMPLATE_REMOVED,
    NO_NON_PRIMITIVE_TYPES
}
