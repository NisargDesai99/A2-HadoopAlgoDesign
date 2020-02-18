import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;


public class Main {

    public static void main(String[] args) throws Exception {

        Configuration conf = new Configuration();

        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();

        if (otherArgs.length != 3) {
            System.err.println("Usage: Main <inputfile hdfs path> <outputfile hdfs path>");
            for (String arg : otherArgs) {
                System.out.println(otherArgs);
            }
            System.exit(2);
        }

        Job job = new Job(conf, "Main");
        job.setJarByClass(Main.class);
        job.setMapperClass(Map.class);
        job.setReducerClass(Reduce.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(otherArgs[1]));
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[2]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

//    public static class Map extends Mapper<KEYIN, VALUEIN, KEYOUT, VALUEOUT>

    public static class Map extends Mapper<LongWritable, Text, Text, Text> {

        private Text word = new Text();

        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String[] line = value.toString().split("\t");
            if (line.length == 2) {
                String user = line[0];
                List<String> values = Arrays.asList(line[1].split(","));
                for (String friend : values) {
                    int u = Integer.parseInt(user);
                    int f = Integer.parseInt(friend);

                    if (u < f) {
                        word.set(u + "," + f);
                    } else {
                        word.set(f + "," + u);
                    }

                    context.write(word, new Text(line[1]));
                }
            }
        }

    }

    public static class Reduce extends Reducer<Text, Text, Text, Text> {

        private Text result = new Text();

        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            HashMap<String, Integer> map = new HashMap<String, Integer>();
            StringBuilder sb = new StringBuilder();
            for (Text friends : values) {
                List<String> temp = Arrays.asList(friends.toString().split(","));
                for (String friend : temp) {
                    if (map.containsKey(friend))
                        sb.append(friend + ',');
                    else
                        map.put(friend, 1);

                }
            }
            if (sb.lastIndexOf(",") > -1) {
                sb.deleteCharAt(sb.lastIndexOf(","));
            }

            result.set(new Text(sb.toString()));
            context.write(key, result);
        }

    }


}
