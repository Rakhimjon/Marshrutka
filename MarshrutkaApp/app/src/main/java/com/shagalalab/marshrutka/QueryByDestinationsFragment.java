package com.shagalalab.marshrutka;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.shagalalab.marshrutka.data.DestinationPoint;
import com.shagalalab.marshrutka.data.Route;
import com.shagalalab.marshrutka.db.DbHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by aziz on 7/10/15.
 */
public class QueryByDestinationsFragment extends Fragment {

    private static final String TAG = "marshrutka";

    private ListView mListView;
    ArrayList<DestinationPoint> mStartDestinationPoints, mEndDestinationPoints;
    DbHelper mDbHelper;
    Spinner mStartPoint, mEndPoint;
    boolean mIsInterfaceCyrillic;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_query_by_destination, null);
        mListView = (ListView) view.findViewById(android.R.id.list);

        mDbHelper = DbHelper.getInstance(getActivity());
        mIsInterfaceCyrillic = ((App)getActivity().getApplicationContext()).isCurrentLocaleCyrillic();

        mStartDestinationPoints = new ArrayList<DestinationPoint>(Arrays.asList(mDbHelper.destinationPoints));
        Comparator<DestinationPoint> comparator = mIsInterfaceCyrillic ? DestinationPoint.QQ_CYR_COMPARATOR
                                                                      : DestinationPoint.QQ_LAT_COMPARATOR;
        Collections.sort(mStartDestinationPoints, comparator);
        mStartDestinationPoints.add(0, new DestinationPoint(-1, getString(R.string.choose_destination), getString(R.string.choose_destination)));

        mStartPoint = (Spinner) view.findViewById(R.id.spinner_start_point);
        mEndPoint = (Spinner) view.findViewById(R.id.spinner_end_point);

        DestinationPointsAdapter startPointAdapter = new DestinationPointsAdapter(getActivity(),
                0, mStartDestinationPoints);

        mStartPoint.setAdapter(startPointAdapter);

        mStartPoint.setOnItemSelectedListener(new StartPointItemSelectedListener());
        mEndPoint.setOnItemSelectedListener(new EndPointItemSelectedListener());

        return view;
    }

    private class StartPointItemSelectedListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
            int startPointPosition = mStartPoint.getSelectedItemPosition();
            int startPointID = mStartDestinationPoints.get(startPointPosition).ID;
            if (startPointID == -1) {
                mEndPoint.setAdapter(null);
                mListView.setAdapter(null);
            } else {
                mEndDestinationPoints = getDestinationListFromArray(mDbHelper.reachableDestinations[startPointID].reachableDestinationIds);
                DestinationPointsAdapter endPointAdapter = new DestinationPointsAdapter(getActivity(),
                        0, mEndDestinationPoints);
                mEndPoint.setAdapter(endPointAdapter);
                setListAdapter();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    }

    private class EndPointItemSelectedListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
            setListAdapter();
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    }

    private void setListAdapter() {
        int startPointPosition = mStartPoint.getSelectedItemPosition();
        int startPointID = mStartDestinationPoints.get(startPointPosition).ID;

        int endPointPosition = mEndPoint.getSelectedItemPosition();
        int endPointID = mEndDestinationPoints.get(endPointPosition).ID;

        int[] routeIds;
        if (startPointID == -1) {
            routeIds = mDbHelper.reverseRoutes[endPointID].routeIds;
        } else if (endPointID == -1) {
            routeIds = mDbHelper.reverseRoutes[startPointID].routeIds;
        } else {
            routeIds = mergeRoutes(mDbHelper.reverseRoutes[endPointID].routeIds,
                    mDbHelper.reverseRoutes[startPointID].routeIds);
        }
        int routesCount = routeIds.length;
        final Route[] filteredRoutes = new Route[routesCount];
        for (int i = 0; i < routesCount; i++) {
            filteredRoutes[i] = mDbHelper.routes[routeIds[i]];
        }
        DestinationsAdapter adapter = new DestinationsAdapter(getActivity(), 0, filteredRoutes, mIsInterfaceCyrillic);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Intent intent = new Intent(getActivity(), DetailActivity.class);
                intent.putExtra(DetailActivity.ROUTE_ID, filteredRoutes[position].ID);
                startActivity(intent);
            }
        });
    }

    private ArrayList<DestinationPoint> getDestinationListFromArray(int[] destIds) {
        ArrayList<DestinationPoint> destinationPointList = new ArrayList<>();
        for (int destId : destIds) {
            destinationPointList.add(mDbHelper.destinationPoints[destId]);
        }
        Comparator<DestinationPoint> comparator = mIsInterfaceCyrillic ? DestinationPoint.QQ_CYR_COMPARATOR
                : DestinationPoint.QQ_LAT_COMPARATOR;
        Collections.sort(destinationPointList, comparator);
        destinationPointList.add(0, new DestinationPoint(-1, getString(R.string.choose_destination), getString(R.string.choose_destination)));
        return destinationPointList;
    }

    private int[] mergeRoutes(int[] routeIds1, int[] routeIds2) {
        Arrays.sort(routeIds1);
        Arrays.sort(routeIds2);
        ArrayList<Integer> merged = new ArrayList<>();
        int pointer1 = 0;
        int pointer2 = 0;
        while (pointer1 < routeIds1.length && pointer2 < routeIds2.length) {
            if (routeIds1[pointer1] == routeIds2[pointer2]) {
                merged.add(routeIds1[pointer1]);
                pointer1++;
                pointer2++;
            } else if (routeIds1[pointer1] > routeIds2[pointer2]) {
                pointer2++;
            } else {
                pointer1++;
            }
        }
        int mergedSize = merged.size();
        int[] mergedRoutes = new int[mergedSize];
        for (int i = 0; i < mergedSize; i++) {
            mergedRoutes[i] = merged.get(i);
        }
        return mergedRoutes;
    }

    private class DestinationPointsAdapter extends ArrayAdapter<DestinationPoint> {
        LayoutInflater mInflater;

        public DestinationPointsAdapter(Context context, int resource, List<DestinationPoint> objects) {
            super(context, resource, objects);
            mInflater = LayoutInflater.from(context);
        }

        private View getCustomView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(android.R.layout.simple_list_item_1, null);
            }

            String text = getItem(position).getName(mIsInterfaceCyrillic);
            ((TextView) convertView).setText(text);
            return convertView;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        @Override
        public long getItemId(int position) {
            DestinationPoint point = getItem(position);
            return point.ID;
        }
    }
}