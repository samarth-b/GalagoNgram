// BSD License (http://www.galagosearch.org/license)

package org.galagosearch.core.parse;

import org.galagosearch.tupleflow.IncompatibleProcessorException;
import org.galagosearch.tupleflow.InputClass;
import org.galagosearch.tupleflow.Linkage;
import org.galagosearch.tupleflow.NullProcessor;
import org.galagosearch.tupleflow.OutputClass;
import org.galagosearch.tupleflow.Processor;
import org.galagosearch.tupleflow.Source;
import org.galagosearch.tupleflow.Step;
import org.galagosearch.tupleflow.execution.Verified;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import org.tartarus.snowball.ext.englishStemmer;

/**
 *
 * @author trevor
 */
@Verified
@InputClass(className = "org.galagosearch.core.parse.Document")
@OutputClass(className = "org.galagosearch.core.parse.Document")
public class Porter2Stemmer implements Processor<Document>, Source<Document> {
    englishStemmer stemmer = new englishStemmer();
    HashMap<String, String> cache = new HashMap();
    public Processor<Document> processor = new NullProcessor(Document.class);

    public void process(Document document) throws IOException {
        List<String> words = document.terms;

        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
           
            if (word != null) {
            String[] myterms = word.split("~");
                for(int j =0; j < myterms.length; j++)
                    {           
                        if(cache.containsKey(myterms[j])) 
                            {
                              myterms[j] = cache.get(myterms[j]);
                            }
                else
                {
                    stemmer.setCurrent(myterms[j]);
                    if (stemmer.stem()) {
                        String stem = stemmer.getCurrent();
                        myterms[j] = stem;
                        cache.put(myterms[j], stem);
                    } 
                    else {
                        cache.put(myterms[j],myterms[j]);
                    }
                }

                if (cache.size() > 50000) {
                    cache.clear();
                }
            }
                String temp = "";
                for(String myterm: myterms)
                    temp+= myterm + "~";
                words.set(i, temp.substring(0,temp.length()-1));
            
            }
        }

        processor.process(document);
    }

    public void setProcessor(Step processor) throws IncompatibleProcessorException {
        Linkage.link(this, processor);
    }

    public void close() throws IOException {
        processor.close();
    }
}
