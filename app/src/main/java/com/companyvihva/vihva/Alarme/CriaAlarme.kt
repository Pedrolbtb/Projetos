package com.companyvihva.vihva.Alarme

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.companyvihva.vihva.Inicio.Inicio
import com.companyvihva.vihva.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Calendar

class CriaAlarme : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var documentId: String? = null
    private val PERMISSION_REQUEST_CODE = 100
    private var horaDiariamente: String? = null // Inicialize como null se não for passado na Intent
    private var nome: String? = null // Variável nome para o remédio

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cria_alarme)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Recuperando o documentId e outros extras da Intent
        documentId = intent.getStringExtra("documentId")
        horaDiariamente = intent.getStringExtra("horaDiariamente")
        val frequencia = intent.getStringExtra("frequencia")
        val horaemhora = intent.getStringExtra("horaemhora")
        val duracao = intent.getStringExtra("duracao")
        val data = intent.getStringExtra("data")
        val estoque = intent.getStringExtra("estoque")
        val lembreme = intent.getStringExtra("lembreme")
        val tipomed = intent.getStringExtra("tipomed")
        val switchEstoqueChecked = intent.getBooleanExtra("switchEstoque", false)
        nome = intent.getStringExtra("remedioId") // Usando nome em vez de Nome

        val editnomeAlarme = findViewById<TextView>(R.id.layout_nome_alarme)
        editnomeAlarme.text = nome ?: "Nome não disponível"

        val editDescAlarme = findViewById<EditText>(R.id.edit_descAlarme)

        val descProgramacao = findViewById<TextView>(R.id.descprogramacao)
        descProgramacao.text = "$frequencia - $horaemhora - $duracao - $data"

        val descEstoque = findViewById<TextView>(R.id.descEstoque)
        descEstoque.text = "$estoque $tipomed"

        // Configurando os listeners
        findViewById<View>(R.id.container_programacaoRemedio).setOnClickListener {
            val telaConfigFrequencia = Intent(this, ConfigFrequencia::class.java).apply {
                putExtra("horaDiariamente", horaDiariamente)
                putExtra("data", data)
                putExtra("duracao", duracao)
                putExtra("frequencia", frequencia)
                putExtra("horaemhora", horaemhora)
                putExtra("lembreme", lembreme)
                putExtra("tipomed", tipomed)
                putExtra("estoque", estoque)
                putExtra("remedioId", nome)
                putExtra("switchEstoque", switchEstoqueChecked)
                putExtra("documentId", documentId)
            }
            startActivity(telaConfigFrequencia)
        }

        findViewById<View>(R.id.container_estoque).setOnClickListener {
            val telaConfigEstoque = Intent(this, ConfigEstoque::class.java).apply {
                putExtra("horaDiariamente", horaDiariamente)
                putExtra("data", data)
                putExtra("duracao", duracao)
                putExtra("frequencia", frequencia)
                putExtra("horaemhora", horaemhora)
                putExtra("lembreme", lembreme)
                putExtra("tipomed", tipomed)
                putExtra("estoque", estoque)
                putExtra("remedioId", nome)
                putExtra("switchEstoque", switchEstoqueChecked)
                putExtra("documentId", documentId)
            }
            startActivity(telaConfigEstoque)
        }

        findViewById<Button>(R.id.btn_salvarAlarme).setOnClickListener {
            observacao()
            requestAlarmPermissionsAndSchedule()

            val inicio = Intent(this, Inicio::class.java)
            startActivity(inicio)
        }


        findViewById<ImageButton>(R.id.btnVoltar).setOnClickListener {
            val estoque = Intent(this, ConfigEstoque::class.java)
            startActivity(estoque)
        }
    }

    private fun observacao() {
        val user = auth.currentUser
        user?.let {
            val userId = it.uid
            val userDocRef = db.collection("clientes").document(userId)
            val alarmeDocRef = userDocRef.collection("Alarmes").document(documentId ?: return)

            // Obtém o texto da EditText
            val editText = findViewById<EditText>(R.id.edit_descAlarme)
            val descricaoAlarme = editText.text.toString()

            // Cria um mapa de dados para salvar no Firebase
            val alarmeData = hashMapOf(
                "descricao" to descricaoAlarme
            )

            // Salva os dados no documento
            alarmeDocRef.set(alarmeData, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "Alarme salvo com sucesso!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Erro ao salvar o alarme: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestAlarmPermissionsAndSchedule() {
        val permissions = arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions, PERMISSION_REQUEST_CODE)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = getSystemService(AlarmManager::class.java)
                if (!alarmManager.canScheduleExactAlarms()) {
                    startActivity(Intent().apply {
                        action = android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    })
                    return
                }
            }
            agendarAlarme()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun agendarAlarme() {
        Log.d("AgendarAlarme", "Iniciando agendamento de alarme")

        // Extrai a hora e minuto de 'horaDiariamente'
        val parts = horaDiariamente?.split(":")
        val hour = parts?.get(0)?.toIntOrNull() ?: 0
        val minute = parts?.get(1)?.toIntOrNull() ?: 0

        // Obtem o tempo atual
        val currentTime = System.currentTimeMillis()

        // Configura o calendário para o próximo horário em que o alarme deve disparar
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTime
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Se o horário for anterior ao atual, define para o próximo dia
            if (timeInMillis <= currentTime) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val triggerTime = calendar.timeInMillis

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Alarme que dispara o toque
        val alarmIntent = Intent(this, AlarmeToque::class.java)
        val alarmPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Abrir a Activity para desligar o alarme, passando o nome do remédio
        val desligaAlarmeIntent = Intent(this, DesligarAlarme::class.java).apply {
            putExtra("remedioNome", nome) // Passa o nome do remédio como extra
        }
        val desligaAlarmePendingIntent = PendingIntent.getActivity(
            this,
            1,
            desligaAlarmeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Agendando o alarme que dispara o toque
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                alarmPendingIntent
            )

            // Agendando a Activity que permitirá desligar o alarme
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime + 10000, // 10 segundos depois do alarme tocar
                desligaAlarmePendingIntent
            )

            Toast.makeText(this, "Alarme agendado para $horaDiariamente!", Toast.LENGTH_SHORT).show()
            Log.d("AgendarAlarme", "Alarme agendado com sucesso para $horaDiariamente")
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao agendar o alarme: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("AgendarAlarme", "Erro ao agendar alarme", e)
        }
    }

    //animaçõa da tela
    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        fragmentTransaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)

        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }
}
