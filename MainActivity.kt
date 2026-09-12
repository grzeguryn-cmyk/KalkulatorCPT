package pl.kalkulator.stawek

import android.app.*
import android.os.Bundle
import android.graphics.Color
import android.content.Context
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

data class Day(val id:Long,val date:String,val meters:Double,val base:Double,val bonus:Double,val total:Double)

class MainActivity: AppCompatActivity() {
    private lateinit var meters: EditText
    private lateinit var total: TextView
    private lateinit var detail: TextView
    private lateinit var historyBox: LinearLayout
    private lateinit var weekText: TextView
    private lateinit var monthText: TextView
    private val days=mutableListOf<Day>()
    private val prefs by lazy { getSharedPreferences("history", Context.MODE_PRIVATE) }

    override fun onCreate(b:Bundle?){super.onCreate(b); load(); ui(); refresh()}

    private fun ui(){
        val scroll=ScrollView(this)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(18,18,18,30);setBackgroundColor(Color.rgb(245,247,250))}
        fun label(s:String,size:Float)=TextView(this).apply{text=s;textSize=size;setTextColor(Color.rgb(35,40,45));setPadding(4,7,4,7)}
        val title=label("Kalkulator stawek",28f); title.setTypeface(null,1); root.addView(title)
        root.addView(label("Dzisiejszy wynik",15f))
        val input=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        meters=EditText(this).apply{
            hint="0";textSize=34f;gravity=Gravity.CENTER;inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setSingleLine();setPadding(8,2,8,2)
        }
        input.addView(meters,LinearLayout.LayoutParams(0,70,1f))
        input.addView(label(" metrów",18f),LinearLayout.LayoutParams(100,70))
        root.addView(input)
        val calc=Button(this).apply{text="OBLICZ";textSize=17f;setOnClickListener{calculate()}}
        root.addView(calc)
        total=label("0,00 zł",38f);total.gravity=Gravity.CENTER;total.setTypeface(null,1);root.addView(total)
        detail=label("",15f);detail.gravity=Gravity.CENTER;root.addView(detail)
        val save=Button(this).apply{text="＋ ZAPISZ DZISIEJSZY DZIEŃ";setOnClickListener{saveDay()}}
        root.addView(save)

        root.addView(label("PODSUMOWANIE",20f).apply{setTypeface(null,1)})
        weekText=label("",16f);monthText=label("",16f);root.addView(weekText);root.addView(monthText)

        root.addView(label("HISTORIA",20f).apply{setTypeface(null,1)})
        historyBox=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};root.addView(historyBox)

        val clear=Button(this).apply{text="WYCZYŚĆ HISTORIĘ";setOnClickListener{
            AlertDialog.Builder(this@MainActivity).setTitle("Wyczyścić historię?")
                .setMessage("Wszystkie zapisane dni zostaną usunięte.").setNegativeButton("ANULUJ",null)
                .setPositiveButton("WYCZYŚĆ"){_,_->days.clear();save();refresh()}.show()
        }}
        root.addView(clear)
        root.addView(label("Stawki dzienne\n0–30 m: 4,00 zł/m\n31–55 m: 7,50 zł/m\npowyżej 55 m: 9,00 zł/m\nDodatkowo: +70% od kwoty podstawowej",14f))
        scroll.addView(root);setContentView(scroll)
    }

    private fun calc(m:Double):DoubleArray{
        val a=min(m,30.0);val b=min(max(m-30,0.0),25.0);val c=max(m-55,0.0)
        val p1=a*4;val p2=b*7.5;val p3=c*9;val base=p1+p2+p3;val bonus=base*.7
        return doubleArrayOf(p1,p2,p3,base,bonus,base+bonus)
    }
    private fun getM():Double?=meters.text.toString().replace(',','.').toDoubleOrNull()
    private fun calculate(){
        val m=getM();if(m==null||m<0){meters.error="Podaj liczbę metrów";return}
        val x=calc(m);total.text=money(x[5]);detail.text="Podstawa ${money(x[3])}  •  +70% ${money(x[4])}\n${money(x[0])} + ${money(x[1])} + ${money(x[2])}"
    }
    private fun saveDay(){
        val m=getM();if(m==null||m<0){meters.error="Podaj liczbę metrów";return}
        val x=calc(m);val d=SimpleDateFormat("dd.MM.yyyy HH:mm",Locale("pl")).format(Date())
        days.add(0,Day(System.currentTimeMillis(),d,m,x[3],x[4],x[5]));save();refresh();meters.text.clear()
        Toast.makeText(this,"Zapisano dzień: ${money(x[5])}",Toast.LENGTH_SHORT).show()
    }
    private fun refresh(){
        val now=Calendar.getInstance()
        val week=days.filter{isSameWeek(it.id,now)}.sumOf{it.total}
        val month=days.filter{isSameMonth(it.id,now)}.sumOf{it.total}
        weekText.text="Ten tydzień: ${money(week)}"
        monthText.text="Ten miesiąc: ${money(month)}"
        historyBox.removeAllViews()
        if(days.isEmpty()){historyBox.addView(TextView(this).apply{text="Brak zapisanych dni.";textSize=15f;setPadding(4,10,4,15)});return}
        days.forEachIndexed{idx,d->
            val row=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(12,10,12,10);setBackgroundColor(Color.WHITE)}
            val line=TextView(this).apply{text="${d.date}    ${fmt(d.meters)} m";textSize=16f;setTypeface(null,1)}
            val pay=TextView(this).apply{text=money(d.total);textSize=22f;setTextColor(Color.rgb(20,100,60))}
            row.addView(line);row.addView(pay)
            row.setOnLongClickListener{deleteDay(idx);true}
            historyBox.addView(row)
            val sep=Space(this);historyBox.addView(sep,LinearLayout.LayoutParams(1,6))
        }
    }
    private fun deleteDay(i:Int){
        AlertDialog.Builder(this).setTitle("Usunąć dzień?").setMessage("${days[i].date} — ${money(days[i].total)}")
            .setNegativeButton("ANULUJ",null).setPositiveButton("USUŃ"){_,_->days.removeAt(i);save();refresh()}.show()
    }
    private fun isSameMonth(id:Long,c:Calendar):Boolean{val x=Calendar.getInstance().apply{timeInMillis=id};return x.get(Calendar.YEAR)==c.get(Calendar.YEAR)&&x.get(Calendar.MONTH)==c.get(Calendar.MONTH)}
    private fun isSameWeek(id:Long,c:Calendar):Boolean{val x=Calendar.getInstance().apply{timeInMillis=id};return x.get(Calendar.YEAR)==c.get(Calendar.YEAR)&&x.get(Calendar.WEEK_OF_YEAR)==c.get(Calendar.WEEK_OF_YEAR)}
    private fun save(){
        prefs.edit().putString("data",days.joinToString("|"){"${it.id};${it.date};${it.meters};${it.base};${it.bonus};${it.total}"}).apply()
    }
    private fun load(){
        val s=prefs.getString("data","") ?: "";if(s.isBlank())return
        s.split("|").forEach{p->val a=p.split(";");if(a.size==6)try{days.add(Day(a[0].toLong(),a[1],a[2].toDouble(),a[3].toDouble(),a[4].toDouble(),a[5].toDouble()))}catch(_:Exception){}}
    }
    private fun money(v:Double)=String.format(Locale("pl","PL"),"%.2f zł",v)
    private fun fmt(v:Double)=String.format(Locale("pl","PL"),"%.2f",v)
}