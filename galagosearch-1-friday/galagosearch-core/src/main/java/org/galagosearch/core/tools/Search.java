// BSD License (http://www.galagosearch.org/license)

package org.galagosearch.core.tools;

import org.galagosearch.core.retrieval.*;
import java.io.IOException;
import java.util.*;
import org.galagosearch.core.parse.Document;
import org.galagosearch.core.retrieval.query.MyStructuredQuery;
import org.galagosearch.core.retrieval.query.Node;
import org.galagosearch.core.retrieval.query.SimpleQuery;
import org.galagosearch.core.retrieval.query.StructuredQuery;
import org.galagosearch.core.store.DocumentStore;
import org.galagosearch.core.store.SnippetGenerator;
import org.galagosearch.tupleflow.Parameters;

/**
 *
 * @author trevor
 */
public class Search {
    SnippetGenerator generator;
    DocumentStore store;
    Retrieval retrieval;
    Retrieval[] retrievals;
    public int ngram = 0;
    public int orgngram =0;

    public Search(Retrieval[] retrieval,int n, DocumentStore store) {
        this.store = store;
        this.retrievals = retrieval;
        ngram = n;
        orgngram = n;
        generator = new SnippetGenerator();
    }
    
    public Search(Retrieval retrieval, DocumentStore store) {
        this.store = store;
        this.retrieval = retrieval;
        generator = new SnippetGenerator();
    }

    public void close() throws IOException {
        store.close();
        retrieval.close();
    }

    public static class SearchResult {
        public Node query;
        public Node transformedQuery;
        public List<SearchResultItem> items;
    }

    public static class SearchResultItem {
        public double score = 0 ;
        public int rank;
        public String identifier;
        public String displayTitle;
        public String url;
        public Map<String, String> metadata;
        public String summary;
    }

    public String getSummary(Document document, Set<String> query) throws IOException {
       
        if (document.metadata.containsKey("description")) {
            String description = document.metadata.get("description");

            if (description.length() > 10) {
                return generator.highlight(description, query);
            }
        }

        return generator.getSnippet(document.text, query);
    }

    public static Node parseQuery(String query, Parameters parameters) {
                
        String queryType = parameters.get("queryType", "complex");

        if (queryType.equals("simple")) {
            
            System.out.println("simple query parser");
            return SimpleQuery.parseTree(query);
        }

        return StructuredQuery.parse(query);
    }
    
    public static Node MyNgramparseQuery(String query, Parameters parameters) {
        return MyNgramparseQuery(query);
    }
    
     public static Node MyNgramparseQuery(String query) {
       // System.out.println("here:" + query.getDefaultParameter());
        return MyStructuredQuery.myparse(query, true);
    }

    public Document getDocument(String identifier) throws IOException {
        return store.get(identifier);
    }
    
    
    private Map<String[], Long> get_counts(Node tree) {
        
        System.out.println("inside get count");
        
        Map<String[], Long> fieldcounts = new HashMap<String[], Long>();
        String[] fieldList = tree.getParameters().get("fields").split(","); 
            
        for(int i =0 ;i < ngram; i++)
        for(String field: fieldList)
            {
                System.out.println("field: from ngram" + i + "is " + field);
                             
                //fieldcounts.put(new String[]{field, i},)
            }
        return fieldcounts;
    }
     
    public SearchResult runQuery(String query, int startAt, int count, boolean summarize) throws Exception {
       
        if(orgngram != 0)
            return runQueryNgram(query, startAt, count, summarize);
        
        Node tree = parseQuery(query, new Parameters());
        
        Node transformed = retrieval.transformQuery(tree); 
                //System.out.println("kuku doest knw kd.");
        ScoredDocument[] results = retrieval.runQuery(transformed, startAt + count);
        
        SearchResult result = new SearchResult();
        
        Set<String> queryTerms = StructuredQuery.findQueryTerms(tree);
        result.query = tree;
        result.transformedQuery = transformed;
        result.items = new ArrayList();
        
      //  System.out.println("kuku had coffee with sukh.");

        for (int i = startAt; i < Math.min(startAt + count, results.length); i++) {
            String identifier = retrieval.getDocumentName(results[i].document);
            Document document = getDocument(identifier);
            SearchResultItem item = new SearchResultItem();

            item.rank = i + 1;
            item.identifier = identifier;
            item.displayTitle = identifier;

            if (document.metadata.containsKey("title")) {
                item.displayTitle = document.metadata.get("title");
            }
            
             item.displayTitle = "title";
                       
          //  item.displayTitle  = document.question.substring(1,10) + "...";

            if (summarize) {
                       
            if (item.displayTitle != null) {
             item.displayTitle = generator.highlight(item.displayTitle, queryTerms);      
            }

            if (document.metadata.containsKey("url")) {
                item.url = document.metadata.get("url");
            }
                
                item.summary = getSummary(document, queryTerms);
            }

          //  item.metadata = document.metadata;
            result.items.add(item);
        }

        return result;
    }
   
    public SearchResult runQueryNgram(String query, int startAt, int count, boolean summarize) throws Exception {      
        ngram = orgngram;
        int mygram = 0;
                       
        List<ScoredDocument[]> allresults = new ArrayList<ScoredDocument[]>();
        List<Integer> grams = new ArrayList();
        
        
        if(query.trim().isEmpty()) {                    
         //  System.out.println("thau .. no query found");
           
           SearchResult results = new SearchResult();
           SearchResultItem item = new SearchResultItem();
//                
//                item.identifier = "1";
//                item.displayTitle = "No relevant document found.";
//                item.rank = 1;
//                item.summary = "try a different weighting combination or use default";
            results.items.add(item);
//                results.query = new Node();
            return results;
      }
      
        if(retrievals.length < ngram)
            throw new Exception("retrievals.length != ngram");
//        ngram = retrievals.length;
        
       //System.out.println("inside runQyeryNgrams");
       //System.out.println("UNstemmed:" + query.toString());                
      
        Node tree = parseQuery( MyStructuredQuery.mystemmer(query)
                                //query
                                , new Parameters());
      
     // System.out.println("stemmed tree:" + tree.toString());
      
      List<Node> children = tree.getInternalNodes();
      if(children.size() < ngram) ngram = children.size();
     
     if(children.isEmpty()) {
//       tree = parseQuery(query.toLowerCase(), new Parameters());
//                    System.out.println("corrected unstemmed tree:" + tree.toString());
//                children = tree.getInternalNodes();
//                ngram = 1;
//                
         allresults.add(retrievals[0].runQuery(MyNgramparseQuery(tree.getDefaultParameter()), 200));
         grams.add(0);
         return get_result(tree, allresults,grams, startAt, count);
      }     
    else
      {
      List<Node> finalQueryList = new ArrayList<Node>();
       String temp = children.get(0).getDefaultParameter();
       finalQueryList.add(MyNgramparseQuery(children.get(0).getDefaultParameter()));
       grams.add(mygram);
       allresults.add(retrievals[mygram++].runQuery(MyNgramparseQuery(children.get(0).getDefaultParameter()), 200));
       //System.out.println("unigram:" + MyNgramparseQuery(children.get(0).getDefaultParameter()).toString());
              
        for(int i =1; i < ngram -1; i++)
          {
              temp += "~" +  children.get(i).getDefaultParameter();
              finalQueryList.add(MyNgramparseQuery(temp, new Parameters()));
              grams.add(mygram);
              System.out.println();
              allresults.add(retrievals[mygram++].runQuery(MyNgramparseQuery(temp, new Parameters()), 200));
              
          }
        
      // mygram--;
      //System.out.println("mgram is:" +  mygram);
       String remaining_temp = "";
      for(int t =0 ; t <= children.size() - ngram; t++)
      {
          temp = "";
          for(int i = 0; i < ngram; i++)
               temp += "~" + children.get(t + i).getDefaultParameter();
          
          remaining_temp += " " + temp.substring("~".length());
      }
      
    //  System.out.println("remaining_temp: " + remaining_temp);
     
      finalQueryList.add(MyNgramparseQuery(remaining_temp, new Parameters()));
      grams.add(mygram);
     //    System.out.println("p:" +  MyNgramparseQuery(remaining_temp, new Parameters()).toString());
      allresults.add(retrievals[mygram].runQuery(MyNgramparseQuery(remaining_temp, new Parameters()), 200));
      

      System.out.println("List of Queries: " + finalQueryList.size());
      for(Node child:finalQueryList)
         System.out.println("child:" + child.toString());
      
      System.out.println("Results of queries: " + allresults.size());
        for(ScoredDocument[] child:allresults)
               System.out.println("" + child.length);
        
      return get_result(tree, allresults, grams, startAt, count);
          
    }
    
    }
    
    class ValueComparator implements Comparator {

    Map base;
    public ValueComparator(Map base) {
        this.base = base;
    }

    public int compare(Object a, Object b) {

        if((Double)base.get(a) < (Double)base.get(b)) {
        return 1;
        } else if((Double)base.get(a) == (Double)base.get(b)) {
        return 0;
        } else {
        return -1;
        }
    }
    }
    private SearchResult get_result(Node tree, List<ScoredDocument[]> allresults, List<Integer> grams, int startAt, int count) throws Exception {
            
            SearchResult results = new SearchResult();
           Set<String> queryTerms = MyStructuredQuery.findQueryTerms(tree);
           results.query = tree;
           results.transformedQuery = tree;
           results.items = new ArrayList();
           
           Map<String, Double> finalresult  = new TreeMap<String, Double>();
            int g =0;
              for(ScoredDocument[] result:allresults) {
                   // System.out.println("" + result.length);                                      
               for (int i = startAt; i < Math.min(startAt + count, result.length); i++) {
                   String identifier = retrievals[grams.get(g)].getDocumentName(result[i].document);
                  // System.out.println(result[i].toString());
                   
                        if(finalresult.containsKey(identifier))
                        {
                            double score = finalresult.get(identifier);
                            finalresult.put(identifier, score + Math.log(Math.abs(result[i].score)));
                        } else
                           finalresult.put(identifier, Math.log(Math.abs(result[i].score)));
                   }
               g++;
           }  
              
//      System.out.println("finalresult " + finalresult.size());
//        for(String file:finalresult.keySet())
//            System.out.println(file + " " + finalresult.get(file));
//           
        ValueComparator bvc =  new ValueComparator(finalresult);
        Map<String,Double> sorted_map = new TreeMap(bvc);
        sorted_map.putAll(finalresult);
        
        System.out.println("sorted_map.size() = " + sorted_map.size());
        for(String file:sorted_map.keySet())
            System.out.println(file + " " + sorted_map.get(file));
//        
          List<String> res = new ArrayList(sorted_map.keySet());
          List<Double> score = new ArrayList(sorted_map.values());
//        System.out.println("printing res");
//        for(String r:res)
//            System.out.println(r);
        
           if(!sorted_map.isEmpty())
           {
           for (int i = startAt; i < Math.min(startAt + count, sorted_map.size()); i++) {
           
            SearchResultItem item = new SearchResultItem();
            item.rank = i + 1;
            item.identifier = res.get(i);
            item.displayTitle = res.get(i);
            Document document = getDocument(res.get(i));
                
            if (document.metadata.containsKey("title")) {
                item.displayTitle = document.metadata.get("title");
              
            item.displayTitle  = document.title;
            item.score = score.get(i);
            
            if (item.displayTitle != null) {
             item.displayTitle = generator.highlight(item.displayTitle, queryTerms);      
            }

            if (document.metadata.containsKey("url")) {
                item.url = document.metadata.get("url");
            }
            
            item.summary = getSummary(document, queryTerms);

            item.metadata = document.metadata;
            results.items.add(item);

            }
           }
           }
           else
//            if(results.items.isEmpty())
            {
                System.out.println("Nothing found!!!");
                  SearchResultItem item = new SearchResultItem();
                
                item.identifier = "1";
                item.displayTitle = "No relevant document found.";
                item.rank = 1;
                item.summary = "try a different weighting combination or use default";
                results.items.add(item);
                results.query = new Node();
                results.query = new Node();
            }
           
           System.out.println(" results: " + results.items.size());
            return results;
    }
    
}
