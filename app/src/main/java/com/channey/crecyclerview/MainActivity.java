package com.channey.crecyclerview;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private CRecyclerView mRecyclerView;
    public List<String> arr = new ArrayList<String>();
    public CAdapter mAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = (CRecyclerView) findViewById(R.id.crecyclerview);

        initData();
        initRecycler();
    }

    public void initData(){
        for(int i=0;i < 100;i++){
            arr.add(String.valueOf(i));
        }
    }

    public void initRecycler(){
        mAdapter = new MyAdapter(this);
        mAdapter.setData(arr);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setOnRefreshListener(new CRecyclerView.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Toast.makeText(MainActivity.this, "Refreshing", Toast.LENGTH_SHORT).show();
                mRecyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mRecyclerView.completeRefresh();
                    }
                },3000);
            }

            @Override
            public void onLoadMore() {
                Toast.makeText(MainActivity.this, "lOADING MORE", Toast.LENGTH_SHORT).show();
                mRecyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        List data = mAdapter.getData();
                        for (int i = 0; i < 10; i++) {
                            data.add(i * 1000);
                        }
                        mAdapter.setData(data);
                        mAdapter.notifyDataSetChanged();
                        mRecyclerView.completeRefresh();
                    }
                },3000);
            }
        });
    }

    class MyAdapter extends CAdapter{
        private Context context;
        public MyAdapter(Context context){
            this.context = context;
        }

        @Override
        protected RecyclerView.ViewHolder onCreateItemViewHolder(ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(R.layout.item,null);
            return new MyViewHolder(view);
        }

        @Override
        protected void onBindItemViewHolder(RecyclerView.ViewHolder holder, int i) {
            MyViewHolder h = (MyViewHolder) holder;
            h.tv.setText(String.valueOf(arr.get(i)));
        }

        @Override
        public int getItemCount() {
            return arr.size();
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder{
        public TextView tv;
        public MyViewHolder(View itemView) {
            super(itemView);
            tv = (TextView) itemView.findViewById(R.id.item_tv);
        }
    }
}
