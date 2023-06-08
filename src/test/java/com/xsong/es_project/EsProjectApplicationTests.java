package com.xsong.es_project;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import com.xsong.es_project.entity.ImageFeature;
import com.xsong.es_project.service.EsSearchService;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
class EsProjectApplicationTests {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Value("${es.queryVectorTemplate}")
    private String queryVectorTemplate;

    @Value("${es.queryVectorKnnTemplate}")
    private String queryVectorKnnTemplate;

    @Autowired
    private EsSearchService esSearchService;

    private static Logger LOG= LoggerFactory.getLogger(EsProjectApplicationTests.class);

    @Test
    void queryById() {
        GetResponse<ImageFeature> response= null;
        try {
            response = elasticsearchClient.get(g -> g
                .index("es_img05")
                .id("2"),
                ImageFeature.class
            );
            if(response.found()){
                ImageFeature imageFeature=response.source();
                LOG.info(response.toString());
                LOG.info("Image name "+imageFeature);
            }else{
                LOG.info("Image not found");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    String queryByIdVector(String index,String id) {
        GetResponse<ImageFeature> response= null;
        try {
            response = elasticsearchClient.get(g -> g
                            .index(index)
                            .id(id),
                    ImageFeature.class
            );
            if(response.found()){
                ImageFeature imageFeature=response.source();
                return imageFeature.getImage_vector().toString();
            }else{
                LOG.info("Image not found");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Test
    void searchByFeature(){
        FileReader fileReader=new FileReader(queryVectorTemplate);
        String template=fileReader.readString();
        Integer searchSize=10;
        String queryVector=queryByIdVector("es_img05","2");
        Map<String,Object> params=new HashMap<String,Object>(){{
            put("searchSize",searchSize);
            put("queryVector",queryVector);
        }};
        String bodyStr = StrUtil.format(template, params);
        Reader body=new StringReader(bodyStr);
        SearchRequest searchRequest=SearchRequest.of(b->b
                .index("es_img05")
                .withJson(body)
        );
        SearchResponse<ImageFeature> response = null;
        try {
            response = elasticsearchClient.search(searchRequest, ImageFeature.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Hit<ImageFeature>> hits = response.hits().hits();
        for(Hit<ImageFeature> hit:hits){
            ImageFeature imageFeature=hit.source();
            imageFeature.setId(hit.id());
            System.out.println(imageFeature.toString());
        }
    }


    @Test
    void searchByFeatureKNN(){
        Map<String,Object> params=new HashMap<String,Object>(){{
            put("k",10);
            put("queryVector","[0.020642, 0.008668, 0.045193, -0.040352, 0.041131, 0.014913, -0.020526, -0.014369, -0.002294, 0.021158, 0.016777, 0.008374, -0.055661, 0.01835, 0.047707, -0.009609, -0.035109, -0.020712, -0.002835, 0.0791, 0.031164, -0.051118, -0.051148, 0.027712, -0.01757, -0.037799, 0.037151, 0.050077, 0.056826, -0.025991, -0.074329, 0.013277, 0.076337, -0.056728, 0.06339, -0.006093, -0.063208, 0.091636, 0.028817, 0.004715, -0.010206, 0.020773, -0.148349, -0.031438, -0.042348, -0.012507, 0.027657, 0.01427, 0.055399, -0.065739, 0.053201, 0.019185, -0.02803, 0.068305, -0.022212, -0.118712, 0.105574, 0.032878, -0.017132, 0.025529, -0.085799, -0.073013, -0.010284, -0.002808, -0.020662, 0.025372, 0.026334, 0.020158, -0.00214, 0.091244, 0.021993, 0.069368, 0.063106, -0.001576, -0.054413, -0.001068, 0.009952, -0.02074, 0.028329, -0.020688, -0.02967, -0.065739, -0.06073, -0.02329, -0.077957, -0.110135, 0.026344, 0.018296, 0.005967, 0.013082, -0.026551, 0.003213, -0.025895, -0.019426, -0.00153, -0.0059, -0.065831, -0.001207, 0.015719, -0.027694, 0.051134, -0.024588, 0.042132, -0.077019, -0.005439, -0.012715, -0.042684, 0.032435, 0.000927, -0.038386, 0.005441, 0.075372, -0.006701, 0.068859, -0.021293, 0.015679, 0.059679, -0.054506, 0.033941, 0.010852, -0.099792, -0.015707, 0.042668, 0.049309, 0.062598, 0.095896, -0.039107, 0.058118, 0.026412, 0.022288, 0.05259, 0.023266, 0.002528, -0.099981, -0.065882, 0.036927, 0.014965, 0.047763, 0.040533, 0.047577, 0.062183, -0.047885, 0.010555, -0.014753, 0.122743, 0.034084, 0.057584, -0.132059, 0.032245, 0.03365, 0.05342, -0.049236, 0.020319, 0.006823, -0.077332, 0.017651, 0.008382, 0.049672, -0.067616, -0.003273, -0.019953, 0.003423, 0.112568, 0.031704, -0.037917, -0.058633, 0.021647, 0.036533, 0.058095, 0.05408, 0.040899, -0.086038, -0.028046, 0.046816, 0.011407, -0.01708, 0.042476, -0.008595, 0.020206, 0.012384, 0.052895, 0.012291, -0.011082, -0.010945, -0.026218, -0.026106, -0.038933, -0.037641, -0.020689, -0.05455, 0.010667, -0.004423, 0.008166, 0.018991, 0.053048, 0.005652, 0.023758, 0.004579, 0.023102, -0.030422, -0.043778, -0.043676, -0.036222, -0.077295, 0.038339, 0.116142, -0.053744, -0.018425, -0.005584, 0.04909, -0.021657, -0.05704, -0.034169, 0.022948, 0.000281, -0.002936, -0.089196, 0.038846, 0.024865, 0.044537, 0.009672, 0.066065, 0.036435, -0.041272, -0.046614, 0.077416, 0.010063, -0.01294, -0.11143, -0.037948, -0.016628, -0.032508, -0.063976, 0.0861, -0.041234, -0.013527, -0.043055, 0.04434, -0.065492, -0.034655, 0.032556, -0.01801, -0.044382, -0.063532, -0.004316, 0.091537, -0.000838, -0.048742, 0.019745, -0.056254, 0.002615, -0.008719, 0.061367, 0.026753, 0.037427, 0.026367, 0.012013, 0.007736, -0.025235, 0.070093, 0.021052, -0.050794, -0.009906, 0.067528, 0.002617, 0.005353, 0.011806, 0.050122, 0.000989, -0.016949, 0.037999, 0.04993, -0.005167, 0.004841, 0.00547, 0.025414, -0.012393, -0.076844, 0.060998, 0.031154, 0.023414, 0.064254, -0.060385, -0.015525, -0.022609, -0.024055, 0.037971, -0.067312, -0.085938, 0.022709, 0.021442, 0.008714, 0.041907, 0.066957, -0.1064, -0.027248, 0.040096, -0.009421, -0.075384, 0.027913, 0.01269, -0.009949, 0.005172, -0.003953, 0.009675, 0.04207, -0.013782, 0.029801, -0.008111, -0.014688, -0.057832, 0.023616, 0.017514, 0.040198, -0.054638, 0.078809, 0.046252, -0.046513, 0.063761, -0.000234, 0.003702, 0.01594, 0.013003, -0.073063, -0.0143, 0.069647, 0.029666, 0.07829, 0.125096, -0.125461, -0.00046, -0.013321, 0.113659, 0.019702, 0.050955, 0.033459, 0.02392, -0.014653, 0.045907, 0.06795, -0.006203, 0.00769, -0.078528, 0.013257, -0.047624, 0.023458, 0.006256, -0.064054, -0.000585, 0.01945, 0.058912, 0.013629, 0.029228, -0.000958, 0.050044, -0.007446, -0.002331, 0.027754, -0.077988, -0.100093, -0.019268, 0.003232, -0.017444, -0.026496, -0.034988, -0.029831, 0.063557, -0.028143, 0.027939, -0.004409, 0.059259, 0.023194, 0.026543, -0.029919, -0.071805, -0.035289, -0.020927, 0.08588, 0.043163, -0.03642, 0.011407, 0.020402, -0.032957, 0.018702, 0.030792, 0.029158, -0.033232, -0.06543, 0.022449, 0.010304, -0.08824, 0.053041, -0.009713, -0.00644, -0.00131, 0.020504, -0.038856, -0.017754, 0.028893, 0.027848, -0.029966, -0.0571, -0.001754, -0.006466, -0.01221, -0.033463, -0.042989, -0.02614, 0.033661, -0.026111, 0.005408, 0.015307, -0.073971, -0.026124, -0.012707, 0.058371, 0.029887, -0.02752, -0.086789, 0.043842, 0.025671, 0.068935, -0.04027, 0.014648, 0.017686, -0.041377, -0.046022, 0.04328, -0.007725, 0.020547, -0.02099, -0.032679, -0.066403, 0.016712, 0.008203, -0.030623, 0.058428, 0.080671, 0.015724, -0.009659, 0.022564, 0.061996, 0.058256, -0.018961, 0.027755, 0.003084, -0.001094, -0.015106, 0.089634, -0.02224, -0.053452, -0.0082, 0.028927, -0.018168, -0.025492, 0.006601, 0.028299, 0.053441, -0.057159, 0.041201, -0.0228, 0.006184, 0.044929, 0.049774, -0.050713, 0.027101, 0.021972, 0.046001, 0.032322, -0.006977, -0.008382, -0.065306, -0.054269, -0.034496, 0.02934, 0.049794, -0.003777, 0.009858, 0.041879, 0.002864, 0.04277, -0.004995, 0.001432, 0.006106, -0.013381, 0.024622, -0.084044, 0.005048, 0.034573, -0.001561, -0.021256, -0.054897, 0.056517, 0.022901, -0.043675, 0.026536, 0.038435, -0.029422, 0.021649, -0.023438, 0.040457, 0.029713, 0.032235, 0.000224, -0.047362, 0.037223, 0.011479, -0.021449, -0.003302, -0.032947, -0.004262, 0.095441]");
        }};
        Date start=new Date();
        List<ImageFeature> imageFeatureList = esSearchService.searchByFeature(params, queryVectorKnnTemplate, "img05_test_02");
        Date end=new Date();
        System.out.println(imageFeatureList);
        Long second=DateUtil.between(start,end, DateUnit.SECOND);
        System.out.println(StrUtil.format("time cost {}s",second));
    }

    @Test
    void regEx(){
        String vectorRegEx="(?<=\\[\\[).*?(?=\\]\\])";
        String s="[[1,2,3]][[3,4,5]]";
        List<String> resultFindAll = ReUtil.findAll(vectorRegEx, s, 0, new ArrayList<String>());
        for(String str:resultFindAll){
            System.out.println(str);
        }
        Image image= null;
        try {
            image = ImageIO.read(new File("D:\\my_project\\es_project\\data\\images\\test.png"));
            String base64= ImgUtil.toBase64DataUri(image,"png");
            System.out.println(base64);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}


//{"knn": {"field": "image_vector","query_vector": [0.020642, 0.008668, 0.045193, -0.040352, 0.041131, 0.014913, -0.020526, -0.014369, -0.002294, 0.021158, 0.016777, 0.008374, -0.055661, 0.01835, 0.047707, -0.009609, -0.035109, -0.020712, -0.002835, 0.0791, 0.031164, -0.051118, -0.051148, 0.027712, -0.01757, -0.037799, 0.037151, 0.050077, 0.056826, -0.025991, -0.074329, 0.013277, 0.076337, -0.056728, 0.06339, -0.006093, -0.063208, 0.091636, 0.028817, 0.004715, -0.010206, 0.020773, -0.148349, -0.031438, -0.042348, -0.012507, 0.027657, 0.01427, 0.055399, -0.065739, 0.053201, 0.019185, -0.02803, 0.068305, -0.022212, -0.118712, 0.105574, 0.032878, -0.017132, 0.025529, -0.085799, -0.073013, -0.010284, -0.002808, -0.020662, 0.025372, 0.026334, 0.020158, -0.00214, 0.091244, 0.021993, 0.069368, 0.063106, -0.001576, -0.054413, -0.001068, 0.009952, -0.02074, 0.028329, -0.020688, -0.02967, -0.065739, -0.06073, -0.02329, -0.077957, -0.110135, 0.026344, 0.018296, 0.005967, 0.013082, -0.026551, 0.003213, -0.025895, -0.019426, -0.00153, -0.0059, -0.065831, -0.001207, 0.015719, -0.027694, 0.051134, -0.024588, 0.042132, -0.077019, -0.005439, -0.012715, -0.042684, 0.032435, 0.000927, -0.038386, 0.005441, 0.075372, -0.006701, 0.068859, -0.021293, 0.015679, 0.059679, -0.054506, 0.033941, 0.010852, -0.099792, -0.015707, 0.042668, 0.049309, 0.062598, 0.095896, -0.039107, 0.058118, 0.026412, 0.022288, 0.05259, 0.023266, 0.002528, -0.099981, -0.065882, 0.036927, 0.014965, 0.047763, 0.040533, 0.047577, 0.062183, -0.047885, 0.010555, -0.014753, 0.122743, 0.034084, 0.057584, -0.132059, 0.032245, 0.03365, 0.05342, -0.049236, 0.020319, 0.006823, -0.077332, 0.017651, 0.008382, 0.049672, -0.067616, -0.003273, -0.019953, 0.003423, 0.112568, 0.031704, -0.037917, -0.058633, 0.021647, 0.036533, 0.058095, 0.05408, 0.040899, -0.086038, -0.028046, 0.046816, 0.011407, -0.01708, 0.042476, -0.008595, 0.020206, 0.012384, 0.052895, 0.012291, -0.011082, -0.010945, -0.026218, -0.026106, -0.038933, -0.037641, -0.020689, -0.05455, 0.010667, -0.004423, 0.008166, 0.018991, 0.053048, 0.005652, 0.023758, 0.004579, 0.023102, -0.030422, -0.043778, -0.043676, -0.036222, -0.077295, 0.038339, 0.116142, -0.053744, -0.018425, -0.005584, 0.04909, -0.021657, -0.05704, -0.034169, 0.022948, 0.000281, -0.002936, -0.089196, 0.038846, 0.024865, 0.044537, 0.009672, 0.066065, 0.036435, -0.041272, -0.046614, 0.077416, 0.010063, -0.01294, -0.11143, -0.037948, -0.016628, -0.032508, -0.063976, 0.0861, -0.041234, -0.013527, -0.043055, 0.04434, -0.065492, -0.034655, 0.032556, -0.01801, -0.044382, -0.063532, -0.004316, 0.091537, -0.000838, -0.048742, 0.019745, -0.056254, 0.002615, -0.008719, 0.061367, 0.026753, 0.037427, 0.026367, 0.012013, 0.007736, -0.025235, 0.070093, 0.021052, -0.050794, -0.009906, 0.067528, 0.002617, 0.005353, 0.011806, 0.050122, 0.000989, -0.016949, 0.037999, 0.04993, -0.005167, 0.004841, 0.00547, 0.025414, -0.012393, -0.076844, 0.060998, 0.031154, 0.023414, 0.064254, -0.060385, -0.015525, -0.022609, -0.024055, 0.037971, -0.067312, -0.085938, 0.022709, 0.021442, 0.008714, 0.041907, 0.066957, -0.1064, -0.027248, 0.040096, -0.009421, -0.075384, 0.027913, 0.01269, -0.009949, 0.005172, -0.003953, 0.009675, 0.04207, -0.013782, 0.029801, -0.008111, -0.014688, -0.057832, 0.023616, 0.017514, 0.040198, -0.054638, 0.078809, 0.046252, -0.046513, 0.063761, -0.000234, 0.003702, 0.01594, 0.013003, -0.073063, -0.0143, 0.069647, 0.029666, 0.07829, 0.125096, -0.125461, -0.00046, -0.013321, 0.113659, 0.019702, 0.050955, 0.033459, 0.02392, -0.014653, 0.045907, 0.06795, -0.006203, 0.00769, -0.078528, 0.013257, -0.047624, 0.023458, 0.006256, -0.064054, -0.000585, 0.01945, 0.058912, 0.013629, 0.029228, -0.000958, 0.050044, -0.007446, -0.002331, 0.027754, -0.077988, -0.100093, -0.019268, 0.003232, -0.017444, -0.026496, -0.034988, -0.029831, 0.063557, -0.028143, 0.027939, -0.004409, 0.059259, 0.023194, 0.026543, -0.029919, -0.071805, -0.035289, -0.020927, 0.08588, 0.043163, -0.03642, 0.011407, 0.020402, -0.032957, 0.018702, 0.030792, 0.029158, -0.033232, -0.06543, 0.022449, 0.010304, -0.08824, 0.053041, -0.009713, -0.00644, -0.00131, 0.020504, -0.038856, -0.017754, 0.028893, 0.027848, -0.029966, -0.0571, -0.001754, -0.006466, -0.01221, -0.033463, -0.042989, -0.02614, 0.033661, -0.026111, 0.005408, 0.015307, -0.073971, -0.026124, -0.012707, 0.058371, 0.029887, -0.02752, -0.086789, 0.043842, 0.025671, 0.068935, -0.04027, 0.014648, 0.017686, -0.041377, -0.046022, 0.04328, -0.007725, 0.020547, -0.02099, -0.032679, -0.066403, 0.016712, 0.008203, -0.030623, 0.058428, 0.080671, 0.015724, -0.009659, 0.022564, 0.061996, 0.058256, -0.018961, 0.027755, 0.003084, -0.001094, -0.015106, 0.089634, -0.02224, -0.053452, -0.0082, 0.028927, -0.018168, -0.025492, 0.006601, 0.028299, 0.053441, -0.057159, 0.041201, -0.0228, 0.006184, 0.044929, 0.049774, -0.050713, 0.027101, 0.021972, 0.046001, 0.032322, -0.006977, -0.008382, -0.065306, -0.054269, -0.034496, 0.02934, 0.049794, -0.003777, 0.009858, 0.041879, 0.002864, 0.04277, -0.004995, 0.001432, 0.006106, -0.013381, 0.024622, -0.084044, 0.005048, 0.034573, -0.001561, -0.021256, -0.054897, 0.056517, 0.022901, -0.043675, 0.026536, 0.038435, -0.029422, 0.021649, -0.023438, 0.040457, 0.029713, 0.032235, 0.000224, -0.047362, 0.037223, 0.011479, -0.021449, -0.003302, -0.032947, -0.004262, 0.095441],"k": 3,"num_candidates": 100}}