package com.hfad.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> content = new ArrayList<>();
    ListView listView ;
    ArrayAdapter arrayAdapter;
    SQLiteDatabase articlesDB;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        articlesDB=this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles(id INTEGER PRIMARY kEY,articleId INTEGER,title VARCHAR,content VARCHAR)");
        listView = findViewById(R.id.listview);
        arrayAdapter=new ArrayAdapter(this,android.R.layout.simple_expandable_list_item_1,titles);
        listView.setAdapter(arrayAdapter);
         downloadTask task =new downloadTask();
         try{
             task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
         }catch(Exception e){
             e.printStackTrace();
         } listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long id) {
                Intent intent=new Intent (getApplicationContext(),articleActivity.class); intent.putExtra("content",content.get(i)); startActivity(intent);
          }
        });updateListView();
    }

    //update a listView
    public void updateListView(){
        Cursor c=articlesDB.rawQuery("SELECT*FROM articles",null);
        int contentIndex=c.getColumnIndex("content");
        int titleIndex=c.getColumnIndex("title");
        if(c.moveToNext()){
        titles.clear();
        content.clear();
        do{
titles.add(c.getString(titleIndex));
content.add(c.getString(contentIndex));}
while(c.moveToFirst());arrayAdapter.notifyDataSetChanged();}}
   //take data from the internet
    public class downloadTask extends AsyncTask<String ,Void,String>{

        @Override
        protected String doInBackground(String... urls) {
            String result ="";
            URL url;
            HttpURLConnection urlConnection=null;
            try{
                url=new URL(urls[0]);
                urlConnection=(HttpURLConnection) url.openConnection();
                InputStream inputStream =urlConnection.getInputStream();
                InputStreamReader inputStreamReader =new InputStreamReader(inputStream);
                int data =inputStreamReader.read();
                while(data != -1){
                    char current =(char) data;
                    result+=current;
                    data =inputStreamReader.read();
                }
                JSONArray jasonarray =new JSONArray(result);
                int numberOfItems=20;
                if(jasonarray.length()<20) {
                    numberOfItems = jasonarray.length();}
                articlesDB.execSQL("DELETE FROM articles");
                    for (int i = 0; i < numberOfItems; i++) {
                        String artcileId = jasonarray.getString(i);
                        url = new URL("https://hacker-news.firebaseio.com/v0/item/" + artcileId + ".json?print=pretty");
                        inputStream = urlConnection.getInputStream();
                        inputStreamReader = new InputStreamReader(inputStream);
                        data = inputStreamReader.read();
                        String articleInfo="";
                        while (data != -1) {
                            char current = (char) data;
                            articleInfo += current;
                            data = inputStreamReader.read();
                        }
                        JSONObject jsonObject=new JSONObject(articleInfo);
                        if(!jsonObject.isNull("title")&& !jsonObject.isNull("url")){
                            String articletitle=jsonObject.getString("title");
                            String articleurl=jsonObject.getString("url");
                            url= new URL(articleurl);
                            urlConnection= (HttpURLConnection) url.openConnection();
                            inputStream=urlConnection.getInputStream();
                            inputStreamReader=new InputStreamReader(inputStream);
                            data=inputStreamReader.read();
                            String artcleContent="";
                            while(data !=-1){
                                char current=(char) data;
                                artcleContent+=current;
                                data=inputStreamReader.read();
                            }
                            String sql="INSERT INTO articles (articleId,title,content)VALUES(?,?,?)";
                            SQLiteStatement statmten=articlesDB.compileStatement(sql);
                            statmten.bindString(1,artcileId);
                            statmten.bindString(2,articletitle);
                            statmten.bindString(3,artcleContent);
                            statmten.execute();
                        }

                    }


        }catch(Exception e){
            e.printStackTrace();}
            return result;
        }

       @Override
       protected void onPostExecute(String s) {
           super.onPostExecute(s); updateListView();
       }
   }

}
