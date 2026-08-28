<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [ <!ENTITY lf "&#x0a;"> ]>

<xsl:stylesheet
    xmlns:j="https://jakarta.ee/xml/ns/jakartaee"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:puml="urn:uuid:00000000-0000-0000-000000000000#"
    version="2.0">
  <xsl:output method="text" indent="no"/>

  <xsl:template match="/">
    <!-- xsl:apply-templates select="j:web-app/j:display-name"/>
    <xsl:text>&lf;</xsl:text>
    <xsl:text>## Context Parameters&lf;</xsl:text>
    <xsl:apply-templates select="j:web-app/j:context-param"/>
    <xsl:text>&lf;</xsl:text>
    <xsl:text>## Filters&lf;</xsl:text>
    <xsl:apply-templates select="j:web-app/j:filter"/>
    <xsl:text>&lf;</xsl:text -->
    <xsl:text>@startuml&lf;</xsl:text>
    <xsl:text>skinparam defaultTextAlignment center&lf;</xsl:text>
    <xsl:text>start&lf;</xsl:text>
    <xsl:apply-templates select="j:web-app/j:filter-mapping"/>
    <xsl:text>stop&lf;</xsl:text>
    <xsl:text>@enduml&lf;</xsl:text>
  </xsl:template>

  <xsl:template match="j:display-name">
    <xsl:text># </xsl:text>
    <xsl:value-of select="."/>
    <xsl:text>&lf;</xsl:text>
  </xsl:template>

  <xsl:template match="j:context-param">
    <xsl:text>* </xsl:text>
    <xsl:value-of select="j:param-name"/>
    <xsl:text> `</xsl:text>
    <xsl:value-of select="j:param-value"/>
    <xsl:text>`</xsl:text>
    <xsl:text>&lf;</xsl:text>
  </xsl:template>

  <xsl:template match="j:filter">
    <xsl:text>* </xsl:text>
    <xsl:value-of select="j:filter-name"/>
    <xsl:text> `</xsl:text>
    <xsl:value-of select="j:filter-class"/>
    <xsl:text>`</xsl:text>
    <xsl:text>&lf;</xsl:text>
  </xsl:template>

  <xsl:template match="j:filter-mapping">
    <xsl:variable name="filterDef" select="//j:filter[j:filter-name = current()/j:filter-name]"/>
    <xsl:if test="j:url-pattern != '/*'">
      <xsl:text>if (request URI matches </xsl:text>
      <xsl:for-each select="j:url-pattern">
        <xsl:value-of select="."/>
        <xsl:if test="position() != last()">
          <xsl:text> OR </xsl:text>
        </xsl:if>
      </xsl:for-each>
      <xsl:text>) then (yes) &lf;</xsl:text>
    </xsl:if>
    <xsl:text>:&lt;b></xsl:text>
    <xsl:value-of select="j:filter-name"/>
    <xsl:text>&lf;</xsl:text>
    <xsl:value-of select="$filterDef/j:filter-class"/>
    <xsl:if test="$filterDef/j:init-param">
      <xsl:text>&lf;</xsl:text>
      <xsl:for-each select="$filterDef/j:init-param">
        <xsl:text>&lf;</xsl:text>
        <xsl:value-of select="j:param-name"/>
        <xsl:text>=</xsl:text>
        <xsl:value-of select="j:param-value"/>
      </xsl:for-each>
    </xsl:if>
    <xsl:text>;&lf;</xsl:text>
    <xsl:if test="j:url-pattern != '/*'">
      <xsl:text>else (no)&lf;</xsl:text>
      <xsl:text>endif&lf;</xsl:text>
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>