package com.example.esp32bluetoothdatadisplay;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LapAdapter extends RecyclerView.Adapter<LapAdapter.LapViewHolder> {

    private Context context;
    private List<Lap> lapList;

    public LapAdapter(Context context, List<Lap> lapList) {
        this.context = context;
        this.lapList = lapList;
    }

    @Override
    public LapViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.lap_item, parent, false);
        return new LapViewHolder(view);
    }

    @Override
    public void onBindViewHolder(LapViewHolder holder, int position) {
        Lap lap = lapList.get(position);
        holder.textViewLapNumber.setText("Lap " + lap.getLapNumber());
        holder.textViewLapTime.setText(lap.getLapTime());
        holder.textViewLapDate.setText(lap.getDate());
        holder.textViewLapDateTime.setText(lap.getTime());

        // Eğer en hızlı tur ise metni farklı renkte göster
        if (lap.isFastest()) {
            holder.textViewLapNumber.setTextColor(context.getResources().getColor(com.google.android.material.R.color.abc_color_highlight_material));
        } else {
            holder.textViewLapNumber.setTextColor(context.getResources().getColor(R.color.black));
        }

        // Tüm TextView'lerin yazı rengini beyaz olarak ayarlayın
        holder.textViewLapTime.setTextColor(context.getResources().getColor(R.color.black));
        holder.textViewLapDate.setTextColor(context.getResources().getColor(R.color.black));
        holder.textViewLapDateTime.setTextColor(context.getResources().getColor(R.color.black));
    }

    @Override
    public int getItemCount() {
        return lapList.size();
    }

    public static class LapViewHolder extends RecyclerView.ViewHolder {
        TextView textViewLapNumber, textViewLapTime, textViewLapDate, textViewLapDateTime;

        public LapViewHolder(View itemView) {
            super(itemView);
            textViewLapNumber = itemView.findViewById(R.id.textViewLapNumber);
            textViewLapTime = itemView.findViewById(R.id.textViewLapTime);
            textViewLapDate = itemView.findViewById(R.id.textViewLapDate);
            textViewLapDateTime = itemView.findViewById(R.id.textViewLapDateTime);
        }
    }
}
