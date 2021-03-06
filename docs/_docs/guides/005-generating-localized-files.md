---
layout: doc
title:  "Pull - Generating Localized Files"
categories: guides
permalink: /docs/guides/generating-localized-files/
---

In this guide, we use `mojito-cli` to generate localized resource files.  Translations in the repository are used to generate localized resource files.  This process is called `pull` in {{ site.mojito }} because translations are "pulled" from {{ site.mojito_green }} to generate localized resource files.

### Pull

Let's say we have the following source resource file `strings.properties` in the current working directory.

```properties
# Greeting from Main UI
hello = Hello!
# Displayed in the Main UI when user logs out.
bye = Goodbye.  Have a nice day!
```


    mojito pull -r MyRepo


This generates localized resource files from the source resource file for each locale defined in `MyRepo` repository.  {{ site.mojito_green }} finds translations for each string in the source resource file and generates localized resource file by replacing source strings with the translations.

In the above example `MyRepo` has four locales `de-DE es-ES fr-FR ja-JP` and therefore four corresponding localized resource files generated in current working directory.

    strings.properties
    strings_de-DE.properties
    strings_es-ES.properties
    strings_fr-FR.properties
    strings_ja-JP.properties

Let's say only Spanish (es-ES) is fully translated in {{ site.mojito_green }}.
![Repository Statistics](./images/repository-statistics-translated.png)


This is the content of `strings_es-ES.properties`.

```properties
# Greeting from Main UI
hello = ¡Hola!
# Displayed in the Main UI when user logs out.
bye = Adiós. ¡Que tengas un buen día!
```

This is the content of `strings_de-DE.properties`.  Note that this is same as the source resource file because German (de-DE) translations are not yet available in {{ site.mojito_green }}.

```properties
# Greeting from Main UI
hello = Hello!
# Displayed in the Main UI when user logs out.
bye = Goodbye.  Have a nice day!
```


### Locale Mapping

    mojito pull -r MyRepo -lm "de:de-DE,es:es-ES,fr:fr-FR,ja:ja-JP"

When generating localized resource files, {{ site.mojito_green }} uses locales configured in the repository.  The locales configured in the repository in the form of language-region, for example, `de-DE`.  However, your project may require localized resource files to be in different locale name format, for example, `de`.  Locale mapping helps you to map your locale name in localized resource files to {{ site.mojito_green }} repository locales.

In the above example `MyRepo` has four locales `de-DE es-ES fr-FR ja-JP` and use `-lm` parameter to generate localized resource files with language code without region.

    strings.properties
    strings_de.properties
    strings_es.properties
    strings_fr.properties
    strings_ja.properties



### Overriding Source and Target Directory

    mojito pull -r MyRepo -s relativeSource -t relativeTarget

    mojito pull -r MyRepo -s /home/explicitSource -t /home/explicitTarget


By default, {{ site.mojito_green }} searches source resource files from current working directory and its sub-directories and generates localized files in the same directory of the source resource files.  You can use `-s` parameter to specify the directory of the source resource files.  Likewise, you can use `-t` parameter to specify where to generate localized resource files.  



### Specific Source File Type

    mojito pull -r MyRepo -ft PROPERTIES


{{ site.mojito_green }} processes all supported source resource files in the working directory by default.  If your working directory has many types of source resource files and if you want to only process specific type, you can use `-ft` parameter.  The above example only generates localized files for Java Properties file.

Available file types are `XLIFF`, `XCODE_XLIFF`, `MAC_STRING`, `MAC_STRINGSDICT`, `ANDROID_STRINGS`, `PROPERTIES`, `PROPERTIES_NOBASENAME`, `PROPERTIES_JAVA`, `RESW`, `RESX`, `PO`, `XTB`, `CSV`, `JS`, `JSON` and `TS`.

The difference between `PROPERTIES` and `PROPERTIES_NOBASENAME` is that the source resource file of `PROPERTIES_NOBASENAME` has source locale name as the file name. For example, `strings.properties` vs. `en.properties`. `PROPERTIES_JAVA` is for the JAVA properties file in ISO_8859-1 encoding with escaped unicode characters.

The `XCODE_XLIFF` is for the xliff files generated by Xcode.


### Overriding Source Locale

    mojito pull -r MyRepo -sl en-US -ft PROPERTIES_NOBASENAME


By default, {{ site.mojito_green }} uses `en` as source locale.  {{ site.mojito_green }} uses soure locale to identity source resource files from localized resource files.  For example, if you have `en.properties` and `en-US.properties` in your working directory, `en.properties` is used as source resource file by default and `en-US.properties` is considered as localized resource file. The above example overrides the default source locale and use `en-US` as source locale using `-sl` parameter.  You must use `-sl` parameter with `-ft` parameter.



### Specific Source File Regex

Let's say you have the following source resource files in working directory.

    release-1.1.xliff
    release-1.2.xliff
    release-2.1.xliff

You can use regular expression to filter source resource files to generate localized resource files from.  The following example only generates localized resource for release-1 related files using `-sr` parameter for regular expression.

    mojito push -r MyRepo -sr "^(release-1).*$"
