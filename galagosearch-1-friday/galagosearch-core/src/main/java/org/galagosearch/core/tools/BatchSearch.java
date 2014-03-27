// BSD License (http://www.galagosearch.org/license)
package org.galagosearch.core.tools;

import java.io.PrintStream;
import java.util.*;
import org.galagosearch.core.parse.Document;
import org.galagosearch.core.retrieval.Retrieval;
import org.galagosearch.core.retrieval.ScoredDocument;
import org.galagosearch.core.retrieval.query.MyStructuredQuery;
import org.galagosearch.core.retrieval.query.Node;
import org.galagosearch.core.retrieval.query.SimpleQuery;
import org.galagosearch.core.retrieval.query.StructuredQuery;
import org.galagosearch.core.tools.Search.SearchResult;
import org.galagosearch.core.tools.Search.SearchResultItem;
import org.galagosearch.tupleflow.Parameters;

/**
 *
 * @author trevor
 */
public class BatchSearch {
    
    static int ngram = 0;
    static int orgngram =0;
    static Retrieval[] retrievals;
    
    public static Node parseQuery(String query, Parameters parameters) {
        String queryType = parameters.get("queryType", "complex");

        if (queryType.equals("simple")) {
            return SimpleQuery.parseTree(query);
        }

        return StructuredQuery.parse(query);
    }

    public static String formatScore(double score) {
        double difference = Math.abs(score - (int) score);

        if (difference < 0.00001) {
            return Integer.toString((int) score);
        }
        return String.format("%10.8f", score);
    }

    public static void run(String[] args, PrintStream out) throws Exception {
        // read in parameters
        Parameters parameters = new Parameters(args);
        List<Parameters.Value> queries = parameters.list("query");

        // open index
        Retrieval retrieval = Retrieval.instance(parameters.get("index"), parameters);

        // record results requested
        int requested = (int) parameters.get("count", 1000);

        // for each query, run it, get the results, look up the docnos, print in TREC format
        for (Parameters.Value query : queries) {
            // parse the query
            
            long start = System.currentTimeMillis();
            String queryText = query.get("text");
            Node queryRoot = parseQuery(queryText, parameters);
            queryRoot = retrieval.transformQuery(queryRoot);

            ScoredDocument[] results = retrieval.runQuery(queryRoot, requested);
            
            long elapsedTimeMillis = System.currentTimeMillis()-start;
            
            for (int i = 0; i < results.length; i++) {
                String document = retrieval.getDocumentName(results[i].document);
                double score = results[i].score;
                int rank = i + 1;

                out.format("%s Q0 %s %d %s galago %f\n", query.get("number"), document, rank,
                           formatScore(score), elapsedTimeMillis/1000F);
            }
        }
    }
    
    public static void runNgram(String[] args, PrintStream out) throws Exception {
        // read in parameters
        Parameters parameters = new Parameters(args);
        List<Parameters.Value> queries = parameters.list("query");

        int requested = (int) parameters.get("count", 10);
        ngram = (int) parameters.get("ngram", 5);
        orgngram = ngram;
        System.out.println("ngram selected:" + ngram);
        // for each query, run it, get the results, look up the docnos, print in TREC format
        
        retrievals = new Retrieval[orgngram];
        for(int i =1; i <= ngram; i++)
            retrievals[i-1] = Retrieval.instance(parameters.get("index") + i, parameters);
        
        for (Parameters.Value query : queries) {
            // parse the query
       long start = System.currentTimeMillis();
       // Get elapsed time in milliseconds
            
       String queryText = query.get("text");
       //System.out.println(queryText);
                              
        SearchResult results = runQueryNgram(queryText, 0, requested,false);

         long elapsedTimeMillis = System.currentTimeMillis()-start;
        // Get elapsed time in seconds
        //System.out.println("Time elapsed: " +  elapsedTimeMillis/1000F);
        
            for (int i = 0; i < results.items.size(); i++) {
                String document = results.items.get(i).identifier;
                double score = results.items.get(i).score;
                int rank = i + 1;

                out.format("%s Q0 %s %d %s King-Julian %f\n", query.get("number"), document, rank,
                           formatScore(score), elapsedTimeMillis/1000F);
            }
        }
    }
    
    
    public static SearchResult runQueryNgram(String query, int startAt, int count, boolean summarize) throws Exception {      
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
            throw new Exception("retrievals.length != ngram " + retrievals.length + ngram);
            //ngram = retrievals.length;
        
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
             // System.out.println();
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
      

//      System.out.println("List of Queries: " + finalQueryList.size());
//      for(Node child:finalQueryList)
//         System.out.println("child:" + child.toString());
//      
//      System.out.println("Results of queries: " + allresults.size());
//        for(ScoredDocument[] child:allresults)
//               System.out.println("" + child.length);
        
      return get_result(tree, allresults, grams, startAt, count);
          
    }
    
    }
    
 
   static class ValueComparator implements Comparator {

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
    private static Search.SearchResult get_result(Node tree, List<ScoredDocument[]> allresults, List<Integer> grams, int startAt, int count) throws Exception {
            
            Search.SearchResult results = new Search.SearchResult();
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
                   //System.out.println(result[i].toString());
                   
                        if(finalresult.containsKey(identifier))
                        {
                            double score = finalresult.get(identifier);
                            finalresult.put(identifier, score + Math.log(Math.abs(result[i].score)));
                        } else
                           finalresult.put(identifier, Math.log(Math.abs(result[i].score)));
                   }
               g++;
           }  
              
//           
        ValueComparator bvc =  new ValueComparator(finalresult);
        Map<String,Double> sorted_map = new TreeMap(bvc);
        sorted_map.putAll(finalresult);
        
//        
          List<String> res = new ArrayList(sorted_map.keySet());
          List<Double> score = new ArrayList(sorted_map.values());
        
           if(!sorted_map.isEmpty())
           {
           for (int i = startAt; i < Math.min(startAt + count, sorted_map.size()); i++) {
           
            Search.SearchResultItem item = new Search.SearchResultItem();
            item.rank = i + 1;
            item.identifier = res.get(i);
            item.score = score.get(i);
            results.items.add(item);

            }
           }
                     
            return results;
    }
    
    
    public static Node MyNgramparseQuery(String query, Parameters parameters) {
        return MyNgramparseQuery(query);
    }
    
     public static Node MyNgramparseQuery(String query) {
       // System.out.println("here:" + query.getDefaultParameter());
        return MyStructuredQuery.myparse(query, true);
    }
    
    public static void main(String[] args) throws Exception {
        run(args, System.out);
    }
}
