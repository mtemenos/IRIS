package com.temenos.interaction.rimdsl.formatting;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;

import javax.inject.Inject;

import org.eclipse.xtext.formatting.INodeModelFormatter;
import org.eclipse.xtext.junit4.InjectWith;
import org.eclipse.xtext.junit4.XtextRunner;
import org.eclipse.xtext.junit4.util.ParseHelper;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.resource.XtextResource;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.temenos.interaction.rimdsl.RIMDslInjectorProvider;
import com.temenos.interaction.rimdsl.rim.DomainModel;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests Formatting.
 *
 * @author aphethean
 */
@RunWith(XtextRunner.class)
@InjectWith(RIMDslInjectorProvider.class)
public class FormatterTest {

    @Inject
    ParseHelper<DomainModel> parser;

    @Inject
    INodeModelFormatter formatter;

    @Test
    public void testFormatting() throws Exception {
        String text = loadTestRIM("Simple.rim");
        DomainModel domainModel = parser.parse(text);
        IParseResult parseResult = ((XtextResource) domainModel.eResource()).getParseResult();
        assertNotNull(parseResult);
        ICompositeNode rootNode = parseResult.getRootNode();
        String formattedText = formatter.format(rootNode, 0, text.length()).getFormattedText();
        assertEquals(toNormalizedLineList(text),toNormalizedLineList(formattedText));
    }

    @Test
    public void testFormattingWithDomain() throws Exception {
        String text = loadTestRIM("TestFormat.rim");
        DomainModel domainModel = parser.parse(text);
        IParseResult parseResult = ((XtextResource) domainModel.eResource()).getParseResult();
        assertNotNull(parseResult);
        ICompositeNode rootNode = parseResult.getRootNode();
        String formattedText = formatter.format(rootNode, 0, text.length()).getFormattedText();
        assertEquals(toNormalizedLineList(text),toNormalizedLineList(formattedText));
    }

    private String loadTestRIM(String rimPath) throws IOException {
        URL url = Resources.getResource(rimPath);
        String rim = Resources.toString(url, Charsets.UTF_8);
        return rim;
    }

    private String toNormalizedLineList(String source) throws IOException {
        StringBuffer lines = new StringBuffer();

        BufferedReader reader = new BufferedReader(new StringReader(source));

        String line = null;
        while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (!line.equals(""))
              lines.append(line.trim()).append('\n');
        }
        return lines.toString();
    }

}
