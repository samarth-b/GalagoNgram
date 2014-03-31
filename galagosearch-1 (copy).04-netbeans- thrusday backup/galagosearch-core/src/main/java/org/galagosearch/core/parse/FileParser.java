// BSD License (http://www.galagosearch.org/license)

package org.galagosearch.core.parse;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.galagosearch.tupleflow.Parameters;

/**
 * Reads data from a single text file of type HTML, XML or txt.
 * 
 * @author trevor
 */
class FileParser implements DocumentStreamParser {
    BufferedReader reader;
    String identifier;

    public FileParser(Parameters parameters, String fileName, BufferedReader bufferedReader) {
        this.identifier = getIdentifier(parameters, fileName);
        this.reader = bufferedReader;
    }

    public String getIdentifier(Parameters parameters, String fileName) {
        String idType = parameters.get("identifier", "filename");
        if (idType.equals("filename")) {
            return fileName;
        } else {
            String id = stripExtensions(fileName);
            id = new File(id).getName();
            return id;
        }
    }

    public String stripExtension(String name, String extension) {
        if (name.endsWith(extension)) {
            name = name.substring(0, name.length()-extension.length());
        }
        return name;
    }

    public String stripExtensions(String name) {
        name = stripExtension(name, ".gz");
        name = stripExtension(name, ".html");
        name = stripExtension(name, ".xml");
        name = stripExtension(name, ".txt");
        return name;
    }

    public String getTitle(String text) {
        int start = text.indexOf("<title>");
        if (start < 0) return "";
        int end = text.indexOf("</title>");
        if (end < 0) return "";
        return new String(text.substring(start + "<title>".length(), end));
    }
    
    
      public String getQuestion(String text) {
        int start = text.indexOf("<question>");
        if (start < 0) return "";
        int end = text.indexOf("</question>");
        if (end < 0) return "";
        return new String(text.substring(start + "<question>".length(), end));
    }
      
      
      public String getAnswers(String text) {
        int start = text.indexOf("<answers>");
        if (start < 0) return "";
        int end = text.indexOf("</answers>");
        if (end < 0) return "";
        return new String(text.substring(start + "<answers>".length(), end));
    }
      
           
        public List<String> getAnswer(String text) {
        text = getAnswers(text);
        List<String> answers = new  ArrayList<String>();
        
        int start = text.indexOf("<answer>");    
        if (start < 0) return answers;
        while(start < text.length())  {              
          int end = text.substring(start).indexOf("</answer>");
          if (end < 0) return answers;
          answers.add(new String(text.substring(start + "<answer>".length(), end)));
          start = end +  "<answer>".length();
            }
        
        return answers;
    }
        
        public String getComments(String text) {
        int start = text.indexOf("<comments>");
        if (start < 0) return "";
        int end = text.indexOf("</comments>");
        if (end < 0) return "";
        return new String(text.substring(start + "<comments>".length(), end));
    }
      
           
        public List<String> getComment(String text) {
        text = getComments(text);
        List<String> comments = new  ArrayList<String>();
        
        int start = text.indexOf("<comment>");    
        
        while(start < text.length())  {              
          int end = text.substring(start).indexOf("</comment>");
          comments.add(new String(text.substring(start + "<comment>".length(), end)));
          start = end +  "<comment>".length();
            }
        
        return comments;
    }

    public Document nextDocument() throws IOException {
        if (reader == null) {
            return null;
        }
        
        StringBuilder builder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            builder.append(line);
            builder.append("\n");
        }

        Document result = new Document();
        result.identifier = identifier;
        result.text = builder.toString();
        result.question = getQuestion(result.text);
        result.answers = getAnswer(result.text);
        result.title = getTitle(result.text);
        
        result.metadata.put("title", result.text);
        //result.metadata.put("title", getTitle(result.text));
        
        
        
        reader.close();
        reader = null;
        return result;
    }
}
