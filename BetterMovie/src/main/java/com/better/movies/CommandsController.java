package com.better.movies;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;
import org.apache.mahout.classifier.BayesFileFormatter;
import org.apache.mahout.classifier.ClassifierResult;
import org.apache.mahout.classifier.bayes.*;
import org.apache.mahout.common.nlp.NGrams;
import org.apache.mahout.vectorizer.DefaultAnalyzer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.*;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Angelo on 17.6.2015 г..
 */
@Controller
@RequestMapping("/bettermovies")
public class CommandsController {
    public static final String COOKIE_FILE_NAME = "zamunda.cookie";
    private Logger logger = Logger.getLogger(CommandsController.class);
    public static final String NEG = "neg";
    public static final String POS = "pos";
    String inputDir  = "D:\\Distrib\\apache-tomcat-8.0.21-windows-x64\\apache-tomcat-8.0.21\\corpus";
    String outputDir = "D:\\Distrib\\apache-tomcat-8.0.21-windows-x64\\apache-tomcat-8.0.21\\corpus\\trainedOutDir";
    String databaseOutputDir = "D:\\Distrib\\apache-tomcat-8.0.21-windows-x64\\apache-tomcat-8.0.21\\corpus\\trainedDb";
    String charset   = "UTF-8";

    private static final int MAX_PAGES_TO_CRAW = 3;

    @RequestMapping(value = "/doTrainingNow", method = RequestMethod.GET)
    @ResponseBody
    public String doTrainingNow() throws Exception {

        training(NEG, inputDir + "\\" + NEG, charset, outputDir);
        training(POS, inputDir + "\\" + POS, charset, outputDir);

        System.out.println("labeling done");
        /**
         * Here we build the bayes parameters object that permits to define some
         * information about the way to stock the training data. Mahout use
         * apache hadoops in background for save the classification data.
         * See the hadoops documentation to know more about this object.
         * Just take care to specify the classifierType and the basePath.
         */

        BayesParameters bayesParameters = buildBayesParam(charset, databaseOutputDir);
        TrainClassifier.trainNaiveBayes(new Path(outputDir), new Path(databaseOutputDir), bayesParameters);
        JSONObject result = new JSONObject();
        result.put("status", "Training Done");
        return result.toString();
    }


    public void training(String label, String dirToClassify, String charset, String outputDir) throws IOException {
        /**
         * Take the document and associate to a label inside a file that
         * respects the apache mahout input format:
         * [LABEL] _TAB_ [TEXT]
         * example:
         * english mahout is a good product.
         * french mahout est un bon produit.
         * Note the analyzer =&gt; This is a lucene analyzer, by default apache mahout provide one. I used this one.
         * In few words the analyzor permits to define how the words will be extracted from your file...
         */

        BayesFileFormatter.format(label, new DefaultAnalyzer(), new File(dirToClassify), Charset.forName(charset), new File(outputDir));
    }

    private BayesParameters buildBayesParam(String charset, String databaseOutputDir) {

        BayesParameters bayesParameters = new BayesParameters();
        bayesParameters.setGramSize(2);
        bayesParameters.set("verbose", "true"); //If you want to see what happen.
        bayesParameters.set("classifierType", "bayes");
        bayesParameters.set("defaultCat", "other"); //The default category to return if a label is not found for a specified text.
        bayesParameters.set("encoding", charset);
        bayesParameters.set("alpha_i", "1.0");
        bayesParameters.set("dataSource", "hdfs");
        bayesParameters.set("basePath", databaseOutputDir);
        return bayesParameters;
    }

    @RequestMapping(value = "/doClassificationNow", method = RequestMethod.POST)
    @ResponseBody
    public String doClassificationNow(@RequestBody String requestBody) throws Exception {
        JSONObject responseRoot = new JSONObject();
        JSONArray arrayResponse = new JSONArray();

        JSONObject jsonObject = new JSONObject(requestBody);
        JSONArray elements = jsonObject.getJSONArray("classify");

        for (int i = 0; i < elements.length(); ++i) {
            JSONObject elem = elements.getJSONObject(i);
            String index = elem.getString("index");
            String data = elem.getString("data");
            String result = classify(data);

            JSONObject classified = new JSONObject();
            classified.put("index", index);
            classified.put("result", result);
            arrayResponse.put(classified);
        }
        responseRoot.put("classified", arrayResponse);

        return responseRoot.toString();
    }

    private String classify(String input) throws Exception {
        ClassifierResult cres = getClassifierResult(input);
        String result = cres.getLabel();
        System.out.println(result + " " + cres.getScore() + " " + input );
        if (result.equals(NEG)){
            return "1";
        } else if (result.equals(POS)) {
            return "0";
        } else {
            return result;
        }
    }

    private ClassifierResult getClassifierResult(String input) throws InvalidDatastoreException, IOException {
        return searchLabel(input, charset, databaseOutputDir);
    }

    /**
     * Ask to mahout to find the good label for the specified content.
     *
     * @param contentToClassify
     *            the content to classify.
     * @param charset
     *            the charset of the content.
     * @param databaseOutputDir
     *            mahout database directory.
     * @return label the label retrieved by mahout.
     * @throws InvalidDatastoreException
     * @throws IOException
     */
    public ClassifierResult searchLabel(String contentToClassify, String charset, String databaseOutputDir) throws InvalidDatastoreException, IOException {
        //define the algorithm to use
        Algorithm algorithm = new BayesAlgorithm();
        //specify the mahout datastore to use. (the path of hadoops database).
        Datastore datastore = new InMemoryBayesDatastore(buildBayesParam(charset, databaseOutputDir));
        //initialize the mahout context.
        ClassifierContext context = new ClassifierContext(algorithm, datastore);
        context.initialize();

        List< String > document = new NGrams( contentToClassify, 2 ).generateNGramsWithoutLabel();

        //Make the search
        return  context.classifyDocument(document.toArray( new String[ document.size() ] ), POS);
    }

    @RequestMapping(value = "/saveTrainingEntries", method = RequestMethod.POST)
    @ResponseBody
    public String saveTrainingEntries(@RequestBody String json) throws Exception {
        JSONObject jsonObject = new JSONObject(json);
        JSONObject storeElem = jsonObject.getJSONObject("store");
        String data = storeElem.getString("data");
        String type = storeElem.getString("type");
        int i = storeElem.getInt("i");
        System.out.println(jsonObject.toString());
        File file;

        JSONObject result = new JSONObject();
        result.put("i", i);
        result.put("type", type);
        if (type.equals(NEG) || type.equals(POS)) {
            file = new File(inputDir + "\\" + type + "\\" + type + "_" + System.currentTimeMillis() + ".txt");
        } else {
            result.put("status", "Server Error - cannot save");
            return result.toString();
        }

        FileUtils.writeStringToFile(file, data, "UTF-8");
        result.put("status", "OK");
        return result.toString();
    }

    @RequestMapping(value = "/doCrawAndGetRating", method = RequestMethod.POST)
    @ResponseBody
    public String doCrawAndGetRating(@RequestBody String json) throws Exception {
        logger.info("doCrawAndGetRating:1:" + json);
        JSONObject jsonObject = new JSONObject(json);

        int i = jsonObject.getInt("i");
        String link = jsonObject.getString("link");

        JSONObject result = new JSONObject();
        result.put("i", i);

        // generate page link with comments
        String linkWithComments = link + "&comments=1";
        // get page

        Map<String, String> cookies = new LinkedHashMap<>();
        cookies = loadCookies();
        if (cookies == null) {

            // go to login page
            // http://zamunda.net/login.php
            Jsoup.connect("http://zamunda.net/login.php").method(Connection.Method.GET);

            // POST to
            // http://zamunda.net/takelogin.php
            Connection.Response response = Jsoup.connect("http://zamunda.net/takelogin.php")
                    .data("username", "annono")
                    .data("password", "1111111111")
                    .method(Connection.Method.POST).execute();
            cookies = response.cookies();

            // save cookies
            FileOutputStream fos = new FileOutputStream(COOKIE_FILE_NAME);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(cookies);
            oos.close();
        }

        Document doc = Jsoup.connect(linkWithComments).cookies(cookies).get();
        // get page count
        int pageCount = 0;
        Elements pageAElements = doc.select("body > table.mainouter > tbody > tr:nth-child(2) > td > table > tbody > tr:nth-child(1) > td:nth-child(3) > p:nth-child(14) > font > a");
        logger.info("doCrawAndGetRating:2:found page elements " + pageAElements.size());
        for (Element oneElement : pageAElements) {
            String href = oneElement.attr("href");
            String page = "page=";
            if (!href.contains(page))
                continue;

            href = href.substring(href.lastIndexOf(page));
            href = href.replace(page, "");
            int biggestPageCandidate;
            try {
                biggestPageCandidate = Integer.parseInt(href);
            } catch (Exception e) {
                continue;
            }
            logger.info("doCrawAndGetRating:3:page candidate - "  + biggestPageCandidate);
            if (biggestPageCandidate > pageCount)
                pageCount = biggestPageCandidate;
        }
        logger.info("doCrawAndGetRating:4:lastPage - " + pageCount);
        pageCount = pageCount > MAX_PAGES_TO_CRAW ? MAX_PAGES_TO_CRAW : pageCount;
        logger.info("doCrawAndGetRating:5: page normalized - " + pageCount);
        //iterate the pages until get reach the last or up to  MAX of 10
        int positiveCount = 0;
        int negativeCount = 0;
        
        while (pageCount >= 0) {
            // get page content
            Document docPage = Jsoup.connect(linkWithComments + "&page=" + pageCount).cookies(cookies).get();
            // get all rowsWithComment as plain text
            Elements rowsWithComment = docPage.select("body > table.mainouter > tbody > tr:nth-child(2) > td > table > tbody > tr:nth-child(1) > td:nth-child(3) > table > tbody > tr > td > table > tbody > tr > td > table.main");

            logger.info("doCrawAndGetRating:6:found rowsWithComment count - " + rowsWithComment.size());
            // for each comment do get label.
            for (Element oneRowWithComment : rowsWithComment) {
                Element oneComment = oneRowWithComment.select("tbody tr td.text").first();

                // handle emoticons
                Elements imgElements = oneComment.select("img");
                for (Element oneImg : imgElements) {
                    String emotag = oneImg.attr("alt");
                    oneImg.replaceWith(new Element(org.jsoup.parser.Tag.valueOf("div"), "").text(emotag));
                }

                String oneCommentTxt = oneComment.text();
                logger.info("doCrawAndGetRating:7:comment stripped - " + oneCommentTxt);
                ClassifierResult classifierResult = getClassifierResult(oneCommentTxt);
                String labelResult = classifierResult.getLabel();
                logger.info("doCrawAndGetRating:8:comment classified - " + labelResult);
                if (labelResult.equals(POS)) {
                    positiveCount++;
                } else {
                    negativeCount++;
                }
            }

            --pageCount;
        }

        // compute ratio
        int rawCount = positiveCount + negativeCount + 1;
        double c = 100 / (double)rawCount;

        long rating = Math.round(positiveCount * c);
        String ratio = String.format("%s", rating);
        logger.info("doCrawAndGetRating:9:finish ratio - " + ratio);
        // return result
        result.put("rate", ratio);
        return result.toString();
    }

    /**
     *
     * @return LinkedHashMap
     */
    private Map<String, String> loadCookies() {
        try {
            FileInputStream fis = new FileInputStream(COOKIE_FILE_NAME);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Map<String, String> anotherMap;
            anotherMap = (LinkedHashMap<String, String>) ois.readObject();
            ois.close();
            return anotherMap;
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }
}
