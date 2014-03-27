// BSD License (http://www.galagosearch.org/license)
package org.galagosearch.core.retrieval.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.galagosearch.tupleflow.Parameters;
import org.tartarus.snowball.ext.englishStemmer;

/**
 * Valid query language syntax:
 *
 * #operator:argument(...)
 * term, or term.field, or term.field.field, etc.
 *
 * @author trevor
 */
public class MyStructuredQuery {
    
    
    static englishStemmer stemmer = new englishStemmer();
    
    private static String stem_queryterm(String term){
        stemmer.setCurrent(term);
        if(stemmer.stem())
        { 
            return stemmer.getCurrent();
        }
        return term;
        
    }
    
    private static String stem_querygram(String term)
    {
//      System.out.println("term:" + term);
        String temp = "";
        for(String t:term.split("~"))
            temp += "~" + stem_queryterm(t);
        
        //System.out.println("after stemmer:" + temp.substring("~".length()));
        return temp.substring("~".length());
    }
    
        private static String stem_query(String term)
    {
        String temp = "";
        
        if(term != null)
        if(term.contains(" "))
        {
            String[] termsplit = term.split(" ");
            for(String t:termsplit)
            temp += " " + stem_querygram(t);
        }
        else
         return stem_querygram(term);
        
        //System.out.println("term: " + term);
        return temp;
    }
    
     private static Node parseZoneBased(Node query) {        
         ArrayList<Node> data = new ArrayList<Node>();
                
         data.add(myinside(query, "title"));
         data.add(myinside(query, "questions"));
         data.add(myinside(query, "answers"));
         data.add(myinside(query, "comments"));
         
         Parameters weights = new Parameters();
         weights.set("0", "0.15");
         weights.set("1", "0.40");
         weights.set("2", "0.40");
         weights.set("3", "0.05");

         return new Node("combine", weights, data, query.getPosition());
     }   
     private static Node parse(Node query) {        
         ArrayList<Node> data = new ArrayList<Node>();
         data.add(myinside(query));
         //System.out.println(new Node("combine", weights, data, query.getPosition()).toString());
         return new Node("combine", new Parameters(), data, 0);
     }
     
     public static Node myparse(Node query, boolean ZoneBased) {
         //query = mystemmer(query);
         ArrayList all = new ArrayList();
         if(ZoneBased)
         all.add(parseZoneBased(query));
         else
         all.add(parse(query));
          //System.out.println("parse");
         return combine(all);
     }
     
      public static Node myparse(String query, boolean ZoneBased) {
         //query = mystemmer(query);
          String[] splits = query.split(" ");          
         ArrayList all = new ArrayList();
         
         for(String spl: splits)
         if(spl!= null)
         if(ZoneBased)
         all.add(parseZoneBased(new Node("default",spl)));
         else
         all.add(parse(new Node("default",spl)));
          //System.out.println("parse");
         return combine(all);
     }
     
      
     private static Node combine(ArrayList query)
     {
//         ArrayList<Node> data = new ArrayList<Node>();
//         Parameters par3 = new Parameters();
//         par3.add("default","dirichlet");
//         par3.add("mu", "1000");
//         data.add(new Node("feature", par3, query, 0));
         
        return new Node("combine", new Parameters(), query, 0);
     }
     
     private static Node myinside(Node query, String zone)
     {
         Parameters par1 = new Parameters();
         par1.add("default", query.getDefaultParameter());
         par1.add("part", "stemmedPostings");
         Node extents1 = new Node("extents", par1, new ArrayList(), 0);
         
         Parameters par2 = new Parameters();
         par2.add("default",zone);
         par2.add("part", "extents");
         Node extents2 = new Node("extents", par2, new ArrayList(), 0);
         
         ArrayList<Node> pairs = new ArrayList<Node>();
         pairs.add(extents1);
         pairs.add(extents2);
         
       //  System.out.println("extents 2: "  +  extents2.toString());
       
         Node inside =  new Node("inside", new Parameters(), pairs, 0);
         
         Parameters par3 = new Parameters();
         par3.add("default","dirichlet");
         par3.add("mu", "1000");
         
         ArrayList data = new ArrayList();
         data.add(inside);
         
         return new Node("feature", par3, data, 0);
     }
          private static Node myinside(Node query)
     {
         Parameters par1 = new Parameters();
         par1.add("default",query.getDefaultParameter());
         par1.add("part", "stemmedPostings");
         Node extents1 = new Node("extents", par1, new ArrayList(), 0);
         
         ArrayList<Node> pairs = new ArrayList<Node>();
         pairs.add(extents1);
           
         Parameters par3 = new Parameters();
         par3.add("default","dirichlet");
         par3.add("mu", "1000");
         
         return new Node("feature", par3, pairs, 0);
     }

    private static Node mystemmer(Node query) {
        List<Node> children = query.getInternalNodes();
        ArrayList<Node> stemmedchildren = new ArrayList<Node>();
        for(Node child:children)
            {
            Parameters par1 = new Parameters();
            par1.add("default",stem_query(child.getDefaultParameter()));
            stemmedchildren.add( new Node("extents", par1, new ArrayList(), 0));
            }
        return new Node("default",  new Parameters(), stemmedchildren, 0);        
    }
    
     public static String mystemmer(String query) {
         
        return stem_query(query.toLowerCase());
    }

   public static Set<String> findQueryTerms(Node queryTree) {
        HashSet<String> queryTerms = new HashSet<String>();
            for (Node child : queryTree.getInternalNodes()) {
                queryTerms.addAll(findQueryTerms(child));
            }
            
        return queryTerms;
    }
}