package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf1;

import java.util.List;

public class DamlLfPrinter
{
    public static String print(DamlLf1.Package _package)
    {
        StringBuilder builder = new StringBuilder();
        for(DamlLf1.Module module: _package.getModulesList())
        {
            builder.append("modules {\n");
            print(builder, " ", module, _package);
            builder.append("}\n");
        }
        for(DamlLf1.Type type : _package.getInternedTypesList())
        {
            builder.append("interned_types {\n");
            print(builder, " ", type, _package);
            builder.append("}\n");
        }
        return builder.toString();
    }

    public static String print(DamlLf1.DefTemplate damlLfTemplate, DamlLf1.Package _package)
    {
        StringBuilder builder = new StringBuilder();
        print(builder,"",damlLfTemplate,_package);
        return builder.toString();
    }

    public static void print(StringBuilder builder, String tab, DamlLf1.Module module, DamlLf1.Package _package)
    {
        for(DamlLf1.DefDataType dataType: module.getDataTypesList())
        {
            builder.append(tab);
            builder.append("data_types {\n");
            print(builder, tab + " ", dataType, _package);
            builder.append(tab);
            builder.append("}\n");
        }
        for(DamlLf1.DefValue value: module.getValuesList())
        {
            builder.append(tab);
            builder.append("values {\n");
            print(builder, tab + " ", value, _package);
            builder.append(tab);
            builder.append("}\n");
        }
        for(DamlLf1.DefTemplate defTemplate: module.getTemplatesList())
        {
            builder.append(tab);
            builder.append("template {\n");
            print(builder, tab + " ", defTemplate, _package);
            builder.append(tab);
            builder.append("}\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.DefDataType dataType, DamlLf1.Package _package)
    {
        if (dataType.hasRecord())
        {
            builder.append(tab);
            builder.append("record {\n");
            printFieldsWithType(builder, tab + " ", dataType.getRecord().getFieldsList(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
    }

    private static void printFieldsWithType(StringBuilder builder, String tab, List<DamlLf1.FieldWithType> fields, DamlLf1.Package _package)
    {
        for (DamlLf1.FieldWithType fieldWithType : fields)
        {
            builder.append(tab);
            builder.append("fields {\n");
            if (fieldWithType.hasType())
            {
                builder.append(tab).append(" ");
                builder.append("type {\n");
                print(builder, tab + "  ", fieldWithType.getType(), _package);
                builder.append(tab).append(" ");
                builder.append("}\n");
            }
            if (fieldWithType.hasFieldInternedStr())
            {
                builder.append(tab);
                builder.append("field_interned_str: ").append(_package.getInternedStrings(fieldWithType.getFieldInternedStr())).append("\n");
                builder.append(tab);
            }
            if (fieldWithType.hasFieldStr())
            {
                builder.append(tab);
                builder.append("field_str: ").append(fieldWithType.getFieldStr()).append("\n");
                builder.append(tab);
            }
            builder.append("}\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.DefValue value, DamlLf1.Package _package)
    {
        if (value.hasNameWithType())
        {
            builder.append(tab);
            builder.append("name_with_type {\n");
            print(builder, tab + " ", value.getNameWithType(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if (value.hasExpr())
        {
            builder.append(tab);
            builder.append("expr {\n");
            print(builder, tab + " ", value.getExpr(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        builder.append(tab);
        builder.append("no_party_literals: ").append(value.getNoPartyLiterals()).append("\n");
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.DefValue.NameWithType nameWithType, DamlLf1.Package _package)
    {
        if (nameWithType.hasType())
        {
            builder.append(tab);
            builder.append("type {\n");
            print(builder, tab + " ", nameWithType.getType(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        builder.append(tab);
        builder.append("name_interned_dname: ").append(DamlLfProtoUtils.getName(_package,nameWithType)).append("\n");
    }


    public static void print(StringBuilder builder, String tab, DamlLf1.DefTemplate damlLfTemplate, DamlLf1.Package _package)
    {
        if(damlLfTemplate.hasTyconInternedDname())
        {
            builder.append("tycon_interned_dname: ").append(DamlLfProtoUtils.getTemplateName(_package,damlLfTemplate)).append("\n");
        }
        else if(damlLfTemplate.hasTyconDname())
        {
            builder.append("tycon_dname: ").append(DamlLfProtoUtils.getTemplateName(_package,damlLfTemplate)).append("\n");
        }

        if(damlLfTemplate.hasPrecond())
        {
            builder.append("pre_cond {\n");
            print(builder, " ", damlLfTemplate.getPrecond(), _package);
            builder.append("}\n");
        }
        if(damlLfTemplate.hasAgreement())
        {
            builder.append("agreement {\n");
            print(builder, " ", damlLfTemplate.getAgreement(), _package);
            builder.append("}\n");
        }
        if(damlLfTemplate.hasSignatories())
        {
            builder.append("signatories {\n");
            print(builder, " ", damlLfTemplate.getSignatories(), _package);
            builder.append("}\n");
        }
        if(damlLfTemplate.hasObservers())
        {
            builder.append("observers {\n");
            print(builder, " ", damlLfTemplate.getObservers(), _package);
            builder.append("}\n");
        }
        for(DamlLf1.TemplateChoice choice : damlLfTemplate.getChoicesList())
        {
            builder.append("choice {\n");
            print(builder, " ", choice, _package);
            builder.append("}\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.TemplateChoice choice, DamlLf1.Package _package)
    {
        if(choice.hasNameInternedStr())
        {
            builder.append(tab);
            builder.append("name_interned_str: ").append(_package.getInternedStrings(choice.getNameInternedStr())).append("\n");
        }
        if(choice.hasControllers())
        {
            builder.append(tab);
            builder.append("controllers {\n");
            print(builder, tab + " ", choice.getControllers(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        if(choice.hasObservers())
        {
            builder.append(tab);
            builder.append("observers {\n");
            print(builder, tab + " ", choice.getObservers(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        if(choice.hasUpdate())
        {
            builder.append(tab);
            builder.append("update {\n");
            print(builder, tab + " ", choice.getUpdate(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.Expr expr, DamlLf1.Package _package)
    {
        if(expr.hasCase())
        {
            builder.append(tab);
            builder.append("case {\n");
            print(builder, tab + " ", expr.getCase(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasApp())
        {
            builder.append(tab);
            builder.append("app {\n");
            print(builder, tab + " ", expr.getApp(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasVarInternedStr())
        {
            builder.append(tab);
            builder.append("var_interned_str: ").append(_package.getInternedStrings(expr.getVarInternedStr())).append("\n");
        }
        else if(expr.hasVal())
        {
            builder.append(tab);
            builder.append("val {\n");
            print(builder, tab + " ", expr.getVal(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasPrimCon())
        {
            builder.append(tab);
            builder.append("prim_con: ").append(expr.getPrimCon()).append("\n");
        }
        else if(expr.hasThrow())
        {
            builder.append(tab);
            builder.append("throw {\n");
            print(builder, tab + " ", expr.getThrow(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasRecCon())
        {
            builder.append(tab);
            builder.append("rec_con {\n");
            print(builder, tab + " ", expr.getRecCon(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasRecProj())
        {
            builder.append(tab);
            builder.append("rec_proj {\n");
            print(builder, tab + " ", expr.getRecProj(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasAbs())
        {
            builder.append(tab);
            builder.append("abs {\n");
            print(builder, tab + " ", expr.getAbs(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasStructCon())
        {
            builder.append(tab);
            builder.append("struct_con {\n");
            print(builder, tab + " ", expr.getStructCon(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasTyAbs())
        {
            builder.append(tab);
            builder.append("ty_abs {\n");
            print(builder, tab + " ", expr.getTyAbs(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasTyApp())
        {
            builder.append(tab);
            builder.append("ty_app {\n");
            print(builder, tab + " ", expr.getTyApp(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasToAny())
        {
            builder.append(tab);
            builder.append("to_any {\n");
            print(builder, tab + " ", expr.getToAny(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasFromAny())
        {
            builder.append(tab);
            builder.append("from_any {\n");
            print(builder, tab + " ", expr.getFromAny(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasTypeRep())
        {
            builder.append(tab);
            builder.append("type_rep {\n");
            print(builder, tab + " ", expr.getTypeRep(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasRecUpd())
        {
            builder.append(tab);
            builder.append("rec_upd {\n");
            print(builder, tab + " ", expr.getRecUpd(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasUpdate())
        {
            builder.append(tab);
            builder.append("update {\n");
            print(builder, tab + " ", expr.getUpdate(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasLet())
        {
            builder.append(tab);
            builder.append("let {\n");
            print(builder, tab + " ", expr.getLet(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasCons())
        {
            builder.append(tab);
            builder.append("cons {\n");
            print(builder, tab + " ", expr.getCons(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasPrimLit())
        {
            builder.append(tab);
            builder.append("prim_lit {\n");
            //print(builder, tab + " ", expr.getPrimLit(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasNil())
        {
            builder.append(tab);
            builder.append("nil {\n");
            print(builder, tab + " ", expr.getNil().getType(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasStructProj())
        {
            builder.append(tab);
            builder.append("struct_proj {\n");
            print(builder, tab + " ", expr.getStructProj().getStruct(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasBuiltin())
        {
            builder.append(tab);
            builder.append("builtin: ").append(expr.getBuiltin()).append("\n");
        }
        else if(expr.hasVariantCon())
        {
            builder.append(tab);
            builder.append("variant_con {\n");
            print(builder, tab + " ", expr.getVariantCon(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasEnumCon())
        {
            builder.append(tab);
            builder.append("enum_con { TODO\n");
            //print(builder, tab + " ", expr.getEnumCon(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasVarStr())
        {
            builder.append(tab);
            builder.append("var_str: ");
            builder.append(expr.getVarStr());
            builder.append("\n");
        }
        else if(expr.hasOptionalNone())
        {
            builder.append(tab);
            builder.append("optional_none { TODO\n");
            //print(builder, tab + " ", expr.getOptionalNone(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasOptionalSome())
        {
            builder.append(tab);
            builder.append("optional_some { TODO\n");
            //print(builder, tab + " ", expr.getOptionalSome(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasScenario())
        {
            builder.append(tab);
            builder.append("scenario { TODO\n");
            //print(builder, tab + " ", expr.getScenario(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasToAnyException())
        {
            builder.append(tab);
            builder.append("to_any_exception { TODO\n");
            //print(builder, tab + " ", expr.getToAnyException(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        else if(expr.hasFromAnyException())
        {
            builder.append(tab);
            builder.append("from_any_exception { TODO\n");
            //print(builder, tab + " ", expr.getFromAnyException(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        else {
           throw new RuntimeException("Don't know how to print Expr type!");
        }

        if(expr.hasLocation())
        {
            builder.append(tab);
            builder.append("location {\n");
            //print(builder, tab + " ", expr.getPrimLit(),_package);
            builder.append(tab);
            builder.append("}\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.Expr.VariantCon variantCon, DamlLf1.Package _package)
    {
        if(variantCon.hasTycon())
        {
            builder.append(tab);
            builder.append("tycon {\n");
            print(builder, tab + " ", variantCon.getTycon(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if(variantCon.hasVariantArg())
        {
            builder.append(tab);
            builder.append("variant_arg {\n");
            print(builder, tab + " ", variantCon.getVariantArg(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if(variantCon.hasVariantConInternedStr())
        {
            builder.append(tab);
            builder.append("variant_con_interned_str: ")
                    .append(_package.getInternedStrings(variantCon.getVariantConInternedStr()))
                    .append("\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.Expr.Cons cons, DamlLf1.Package _package)
    {
        if(cons.hasType())
        {
            builder.append(tab);
            builder.append("type {\n");
            print(builder, tab + " ", cons.getType(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        for(DamlLf1.Expr expr: cons.getFrontList())
        {
            builder.append(tab);
            builder.append("front {\n");
            print(builder, tab + " ", expr, _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if(cons.hasTail())
        {
            builder.append(tab);
            builder.append("tail {\n");
            print(builder, tab + " ", cons.getTail(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.Update update, DamlLf1.Package _package)
    {
        if(update.hasBlock())
        {
            builder.append(tab);
            builder.append("block {\n");
            print(builder, tab + " ", update.getBlock(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if(update.hasCreate())
        {
            builder.append(tab);
            builder.append("create {\n");
            //print(builder, tab + " ", update.getCreate(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if(update.hasExercise())
        {
            builder.append(tab);
            builder.append("exercise {\n");
            //print(builder, tab + " ", update.getExercise(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if(update.hasFetch())
        {
            builder.append(tab);
            builder.append("fetch {\n");
            //print(builder, tab + " ", update.getExercise(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if(update.hasEmbedExpr())
        {
            builder.append(tab);
            builder.append("embed_expr {\n");
            //print(builder, tab + " ", update.getExercise(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if(update.hasGetTime())
        {
            builder.append(tab);
            builder.append("get_time {\n");
            //print(builder, tab + " ", update.getExercise(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if(update.hasPure())
        {
            builder.append(tab);
            builder.append("pure {\n");
            //print(builder, tab + " ", update.getExercise(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.Block block, DamlLf1.Package _package)
    {
        for(DamlLf1.Binding binding: block.getBindingsList())
        {
            builder.append(tab);
            builder.append("bindings {\n");
            print(builder, tab + " ", binding, _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if (block.hasBody())
        {
            builder.append(tab);
            builder.append("body {\n");
            print(builder, tab + " ", block.getBody(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.Binding binding, DamlLf1.Package _package)
    {
        if(binding.hasBinder())
        {
            builder.append(tab);
            builder.append("binder {\n");
            print(builder, tab + " ", binding.getBinder(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if(binding.hasBound())
        {
            builder.append(tab);
            builder.append("bound {\n");
            print(builder, tab + " ", binding.getBound(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.Expr.TyApp tyApp, DamlLf1.Package _package)
    {
        if(tyApp.hasExpr())
        {
            builder.append(tab);
            builder.append("expr {\n");
            print(builder, tab + " ", tyApp.getExpr(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        printTypeArgs(builder, tab, tyApp.getTypesList(), _package);
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.Expr.FromAny fromAny, DamlLf1.Package _package)
    {
        if (fromAny.hasType())
        {
            builder.append(tab);
            builder.append("type {\n");
            print(builder, tab + " ", fromAny.getType(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if (fromAny.hasExpr())
        {
            builder.append(tab);
            builder.append("expr {\n");
            print(builder, tab + " ", fromAny.getExpr(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.Expr.ToAny toAny, DamlLf1.Package _package)
    {
        if (toAny.hasType())
        {
            builder.append(tab);
            builder.append("type {\n");
            print(builder, tab + " ", toAny.getType(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if (toAny.hasExpr())
        {
            builder.append(tab);
            builder.append("expr {\n");
            print(builder, tab + " ", toAny.getExpr(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.Expr.TyAbs tyAbs, DamlLf1.Package _package)
    {
        for(DamlLf1.TypeVarWithKind param: tyAbs.getParamList())
        {
            builder.append(tab);
            builder.append("param {\n");
            print(builder, tab + " ", param, _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if (tyAbs.hasBody())
        {
            builder.append(tab);
            builder.append("body {\n");
            print(builder, tab + " ", tyAbs.getBody(), _package);
            builder.append(tab);
            builder.append("}\n");
        }

    }

    private static void print(StringBuilder builder, String tab, DamlLf1.TypeVarWithKind typeVarWithKind, DamlLf1.Package _package)
    {
        if(typeVarWithKind.hasKind())
        {
            builder.append(tab);
            builder.append("kind {\n");
            print(builder, tab + " ", typeVarWithKind.getKind(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.Kind kind, DamlLf1.Package _package)
    {
        if(kind.hasArrow())
        {
            builder.append(tab);
            builder.append("arrow {\n");
            //print(builder, tab + " ", kind.getArrow(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.Expr.StructCon structCon, DamlLf1.Package _package)
    {
        printFieldsWithExpr(builder,tab, structCon.getFieldsList(),_package);
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.Expr.Abs abs, DamlLf1.Package _package)
    {
        for(DamlLf1.VarWithType varWithType : abs.getParamList())
        {
            builder.append(tab);
            builder.append("param {\n");
            print(builder, tab + " ", varWithType, _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if (abs.hasBody())
        {
            builder.append(tab);
            builder.append("body {\n");
            print(builder, tab + " ", abs.getBody(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.VarWithType varWithType, DamlLf1.Package _package)
    {
        if(varWithType.hasType())
        {
            builder.append(tab);
            builder.append("type {\n");
            print(builder, tab + " ", varWithType.getType(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if(varWithType.hasVarInternedStr())
        {
            builder.append(tab);
            builder.append("var_interned_str: ")
                    .append(_package.getInternedStrings(varWithType.getVarInternedStr()))
                    .append("\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.Expr.RecCon recCon, DamlLf1.Package _package)
    {
        if(recCon.hasTycon())
        {
            builder.append(tab);
            builder.append("tycon {\n");
            print(builder, tab + " ", recCon.getTycon(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        printFieldsWithExpr(builder, tab, recCon.getFieldsList(), _package);
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.Expr.RecUpd recUpd, DamlLf1.Package _package)
    {
        if(recUpd.hasTycon())
        {
            builder.append(tab);
            builder.append("tycon {\n");
            print(builder, tab + " ", recUpd.getTycon(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if(recUpd.hasRecord())
        {
            builder.append(tab);
            builder.append("record {\n");
            print(builder, tab + " ", recUpd.getRecord(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if(recUpd.hasUpdate())
        {
            builder.append(tab);
            builder.append("update {\n");
            print(builder, tab + " ", recUpd.getUpdate(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if(recUpd.hasFieldInternedStr())
        {
            builder.append(tab);
            builder.append(" field_interned_str: ")
                    .append(_package.getInternedStrings(recUpd.getFieldInternedStr()))
                    .append("\n");
        }
    }

    private static void printFieldsWithExpr(StringBuilder builder, String tab, List<DamlLf1.FieldWithExpr> fields, DamlLf1.Package _package)
    {
        for(DamlLf1.FieldWithExpr fieldWithExpr: fields)
        {
            builder.append(tab);
            builder.append("fields {\n");
            if(fieldWithExpr.hasExpr())
            {
                builder.append(tab);
                builder.append("expr {\n");
                print(builder, tab + " ", fieldWithExpr.getExpr(), _package);
                builder.append(tab);
                builder.append("}\n");
            }
            if(fieldWithExpr.hasFieldInternedStr())
            {
                builder.append(tab);
                builder.append(" field_interned_str: ")
                        .append(_package.getInternedStrings(fieldWithExpr.getFieldInternedStr()))
                        .append("\n");
            }
            builder.append(tab);
            builder.append("}\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.Expr.RecProj recProj, DamlLf1.Package _package)
    {
        if (recProj.hasTycon())
        {
            builder.append(tab);
            builder.append("tycon {\n");
            print(builder, tab + " ", recProj.getTycon(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if(recProj.hasRecord())
        {
            builder.append(tab);
            builder.append("record {\n");
            print(builder, tab + " ", recProj.getRecord(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if(recProj.hasFieldInternedStr())
        {
            builder.append(tab);
            builder.append("field_interned_str: ")
                    .append(_package.getInternedStrings(recProj.getFieldInternedStr()))
                    .append("\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.Type.Con tycon, DamlLf1.Package _package)
    {
        if(tycon.hasTycon())
        {
            builder.append(tab);
            builder.append("tycon {\n");
            print(builder,tab + " ", tycon.getTycon(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        printTypeArgs(builder, tab, tycon.getArgsList(), _package);
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.Expr.Throw _throw, DamlLf1.Package _package)
    {
        if (_throw.hasReturnType())
        {
            builder.append(tab);
            builder.append("return_type {\n");
            print(builder, tab + " ", _throw.getReturnType(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if (_throw.hasExceptionExpr())
        {
            builder.append(tab);
            builder.append("exception_expr {\n");
            print(builder, tab + " ", _throw.getExceptionExpr(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.Case _case, DamlLf1.Package _package)
    {
        if(_case.hasScrut())
        {
            builder.append(tab);
            builder.append("scrut {\n");
            print(builder,tab + " ", _case.getScrut(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        for(DamlLf1.CaseAlt alt : _case.getAltsList())
        {
            builder.append(tab);
            builder.append("alt {\n");
            print(builder,tab + " ", alt, _package);
            builder.append(tab);
            builder.append("}\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.CaseAlt alt, DamlLf1.Package _package)
    {
        if (alt.hasBody())
        {
            builder.append(tab);
            builder.append("body {\n");
            print(builder, tab + " ", alt.getBody(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if(alt.hasCons())
        {
            builder.append(tab);
            builder.append("cons {\n");
            print(builder, tab + " ", alt.getCons(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.CaseAlt.Cons cons, DamlLf1.Package _package)
    {
        //cons.hasVarHeadInternedStr()
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.Expr.App app, DamlLf1.Package _package)
    {
        if(app.hasFun())
        {
            builder.append(tab);
            builder.append("fun {\n");
            print(builder,tab + " ", app.getFun(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        for(DamlLf1.Expr arg : app.getArgsList())
        {
            builder.append(tab);
            builder.append("args {\n");
            print(builder,tab + " ", arg, _package);
            builder.append(tab);
            builder.append("}\n");
        }

    }

    private static void print(StringBuilder builder, String tab, DamlLf1.TypeConName typeConName, DamlLf1.Package _package)
    {
        if (typeConName.hasModule())
        {
            builder.append(tab);
            builder.append("module {\n");
            print(builder, tab + " ", typeConName.getModule(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        if( typeConName.hasNameInternedDname())
        {
            builder.append(tab).append("name_interned_dname: ").append(DamlLfProtoUtils.getName(_package, typeConName))
                    .append("\n");
        }
        if( typeConName.hasNameDname())
        {
            builder.append(tab).append("name_dname: ").append(DamlLfProtoUtils.getName(_package, typeConName))
                    .append("\n");
        }
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.ValName valName, DamlLf1.Package _package)
    {
        if (valName.hasModule())
        {
            builder.append(tab);
            builder.append("module {\n");
            print(builder, tab + " ", valName.getModule(), _package);
            builder.append(tab);
            builder.append("}\n");
        }
        builder.append(tab).append("name_interned_dname: ").append(DamlLfProtoUtils.getName(_package, valName)).append("\n");
    }

    private static void print(StringBuilder builder, String tab, DamlLf1.ModuleRef moduleRef, DamlLf1.Package _package)
    {
        if(moduleRef.hasPackageRef())
        {
            builder.append(tab);
            builder.append("package_ref {\n");
            if(moduleRef.getPackageRef().hasSelf())
            {
                builder.append(tab).append(" self{\n").append(tab).append(" }\n");
            }
            builder.append(tab);
            builder.append("}\n");
        }
        if(moduleRef.hasModuleNameInternedDname())
        {
            builder.append(tab);
            builder.append("module_name_interned_dname: ")
                    .append(DamlLfProtoUtils.getModuleName(_package, moduleRef))
                    .append("\n");
        }
        else if(moduleRef.hasModuleNameDname())
        {
            builder.append(tab);
            builder.append("module_name_dname: ")
                    .append(DamlLfProtoUtils.getModuleName(_package, moduleRef))
                    .append("\n");
        }
    }

    protected static void print(StringBuilder builder, String tab, DamlLf1.Type type, DamlLf1.Package _package)
    {
        if(type.hasInterned())
        {
            builder.append(tab);
            builder.append("interned: \n");
            print(builder,tab,_package.getInternedTypes(type.getInterned()), _package);
        }
        else
        {
            if (type.hasPrim())
            {
                builder.append(tab);
                builder.append("prim {\n");
                builder.append(tab);
                builder.append(" name: ");
                builder.append(type.getPrim().getPrim().toString()).append("\n");
                printTypeArgs(builder, tab + " ", type.getPrim().getArgsList(), _package);
                builder.append(tab);
                builder.append("}\n");
            }
            else if (type.hasCon())
            {
                builder.append(tab);
                builder.append("con {\n");
                print(builder, tab + " ", type.getCon(), _package);
                builder.append(tab);
                builder.append("}\n");
            }
            else if (type.hasNat())
            {
                builder.append(tab).append(" nat: ").append(type.getNat()).append("\n");
            }
            else if (type.hasSyn())
            {
                builder.append(tab);
                builder.append("syn {\n");
                //type.getSyn().getTysyn()
                printTypeArgs(builder, tab + " ", type.getSyn().getArgsList(), _package);
                builder.append(tab);
                builder.append("}\n");
            }
            else if (type.hasForall())
            {
                builder.append(tab);
                builder.append("forall {\n");
                //type.getForall().getVarsList()
                builder.append(tab);
                builder.append("}\n");
            }
            else if (type.hasVar())
            {
                builder.append(tab);
                builder.append("var {\n");
                if(type.getVar().hasVarInternedStr())
                {
                    builder.append(tab);
                    builder.append(" var_interned_str: ")
                            .append(_package.getInternedStrings(type.getVar().getVarInternedStr()))
                            .append("\n");
                }
                builder.append(tab);
                builder.append("}\n");
            }
            else if (type.hasStruct())
            {
                builder.append(tab);
                builder.append("struct {\n");
                printFieldsWithType(builder, tab + " ", type.getStruct().getFieldsList(), _package);
                builder.append(tab);
                builder.append("}\n");
            }
            else {
                throw new RuntimeException("Oops");
            }
        }
    }

    private static void printTypeArgs(StringBuilder builder, String tab, List<DamlLf1.Type> args, DamlLf1.Package _package)
    {
        for (DamlLf1.Type type : args)
        {
            builder.append(tab);
            builder.append("args {\n");
            print(builder, tab + " ", type, _package);
            builder.append(tab);
            builder.append("}\n");
        }
    }
}
