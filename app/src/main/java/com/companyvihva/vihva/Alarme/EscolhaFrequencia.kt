package com.companyvihva.vihva.Alarme

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import com.companyvihva.vihva.Inicio.Inicio
import com.companyvihva.vihva.R
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class EscolhaFrequencia : AppCompatActivity() {

    private var frequencia: String? = null
    private var intervalo: String? = null
    private var dias: String? = null
    private var duracao = intent?.getStringExtra("duracao")
    private var data = intent?.getStringExtra("data")
    private var estoque = intent?.getStringExtra("estoque")
    private var lembreme = intent?.getStringExtra("lembreme")
    private var tipomed = intent?.getStringExtra("tipomed")
    private var switchEstoqueChecked = intent?.getBooleanExtra("switchEstoque", false) ?: false
    private var nome = intent?.getStringExtra("remedioId")
    private var horaDiariamente = intent?.getStringExtra("horaDiariamente")
    private var horaemhora= intent?.getStringExtra ("horaemhora")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_escolha_frequencia)

        val parentLayout = findViewById<LinearLayout>(R.id.layout_Opcoes)
        val radioGroup: RadioGroup = findViewById(R.id.radioGroup_frequencia)
        val btnVoltar: ImageButton = findViewById(R.id.btnVoltar)
        val btnProx: Button = findViewById(R.id.btn_proximo)

        btnProx.setOnClickListener {
            avanc()
        }
        btnVoltar.setOnClickListener {
            val dados = if (frequencia == "Somente em certos dias") {
                prepararDadosParaEnvio()
            } else {
                Bundle()
            }
            val frequenciaSelecionada = frequencia
            val horaEmHora: String? = if (frequencia == "Intervalo") {
                val editTextHora = parentLayout.findViewById<EditText>(R.id.editTextHoraemHora)
                editTextHora?.text?.toString() ?: ""
            } else {
                null
            }

            val intent = Intent(this, EscolhaRemedio::class.java).apply {
                putExtras(dados)
                putExtra("frequencia", frequenciaSelecionada)
                putExtra("horaemhora", horaEmHora)
                putExtra("duracao", duracao)
                putExtra("data", data)
                putExtra("estoque", estoque)
                putExtra("lembreme", lembreme)
                putExtra("tipomed", tipomed)
                putExtra("remedioId", nome)
                putExtra("switchEstoque", switchEstoqueChecked)
                putExtra("horaDiariamente", horaDiariamente)
            }
            startActivity(intent)
        }

        if (frequencia != null) {
            when (frequencia) {
                "Diariamente" -> radioGroup.check(R.id.radio_diariamente)
                "Sem Alarme" -> radioGroup.check(R.id.radio_nada)
                "Intervalo" -> radioGroup.check(R.id.radio_intervalo)
                "Somente em certos dias" -> radioGroup.check(R.id.radio_dias)
            }
            if (frequencia == "Intervalo") {
                val layoutToAdd = LayoutInflater.from(this)
                    .inflate(R.layout.intervalo_opcoes, null)

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 20, 0)
                layoutToAdd.layoutParams = params
                parentLayout.addView(layoutToAdd)
            } else if (frequencia == "Diariamente") {
                val layoutToAdd = LayoutInflater.from(this)
                    .inflate(R.layout.diariamente_opcoes, null)

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 20, 0)
                layoutToAdd.layoutParams = params
                parentLayout.addView(layoutToAdd)

                layoutToAdd.findViewById<TextView>(R.id.textView_calendarioAlarme)
                    .setOnClickListener {
                        val picker = MaterialTimePicker.Builder()
                            .setTimeFormat(TimeFormat.CLOCK_24H)
                            .setHour(12)
                            .setMinute(10)
                            .setTitleText("Selecione uma hora para tocar")
                            .build()

                        picker.addOnPositiveButtonClickListener {
                            val formattedHour = String.format("%02d", picker.hour)
                            val formattedMinute = String.format("%02d", picker.minute)
                            val selectedTime = "$formattedHour:$formattedMinute"
                            layoutToAdd.findViewById<TextView>(R.id.textView_calendarioAlarme).text =
                                selectedTime
                            horaDiariamente = selectedTime
                        }

                        picker.show(supportFragmentManager, "TAG_TIME_PICKER")
                    }
            }
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            frequencia = when (checkedId) {
                R.id.radio_diariamente -> "Diariamente"
                R.id.radio_nada -> "Sem Alarme"
                R.id.radio_intervalo -> "Intervalo"
                R.id.radio_dias -> "Somente em certos dias"
                else -> null
            }

            // Remove todas as views antes de adicionar novas
            parentLayout.removeAllViews()

            when (frequencia) {
                "Diariamente" -> {
                    val layoutToAdd = LayoutInflater.from(this)
                        .inflate(R.layout.diariamente_opcoes, null)

                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(0, 0, 20, 0)
                    layoutToAdd.layoutParams = params
                    parentLayout.addView(layoutToAdd)

                    layoutToAdd.findViewById<TextView>(R.id.textView_calendarioAlarme)
                        .setOnClickListener {
                            val picker = MaterialTimePicker.Builder()
                                .setTimeFormat(TimeFormat.CLOCK_24H)
                                .setHour(12)
                                .setMinute(10)
                                .setTitleText("Selecione uma hora para tocar")
                                .build()

                            picker.addOnPositiveButtonClickListener {
                                val formattedHour = String.format("%02d", picker.hour)
                                val formattedMinute = String.format("%02d", picker.minute)
                                val selectedTime = "$formattedHour:$formattedMinute"
                                layoutToAdd.findViewById<TextView>(R.id.textView_calendarioAlarme).text =
                                    selectedTime
                                horaDiariamente = selectedTime
                            }

                            picker.show(supportFragmentManager, "TAG_TIME_PICKER")
                        }
                }

                "Intervalo" -> {
                    val layoutToAdd = LayoutInflater.from(this)
                        .inflate(R.layout.intervalo_opcoes, null)

                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(0, 0, 20, 0)
                    layoutToAdd.layoutParams = params
                    parentLayout.addView(layoutToAdd)
                }

                "Somente em certos dias" -> {
                    val layoutToAdd = LayoutInflater.from(this)
                        .inflate(R.layout.dias_opcoes_desativado, null)

                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(23, 0, 0, 0)
                    layoutToAdd.layoutParams = params
                    parentLayout.addView(layoutToAdd)

                    // Configura estilo das CheckBox baseado no estado
                    val checkBoxIds = listOf(
                        R.id.checkbox_domingo,
                        R.id.checkbox_segunda,
                        R.id.checkbox_terca,
                        R.id.checkbox_quarta,
                        R.id.checkbox_quinta,
                        R.id.checkbox_sexta,
                        R.id.checkbox_sabado
                    )

                    checkBoxIds.forEach { checkBoxId ->
                        val checkBox = layoutToAdd.findViewById<CheckBox>(checkBoxId)
                        checkBox?.setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                checkBox.setBackgroundResource(R.drawable.checkbox_dias)
                            } else {
                                checkBox.setBackgroundResource(R.drawable.checkbox_dias_desativado)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun prepararDadosParaEnvio(): Bundle {
        // Aqui você prepara os dados com base nas seleções feitas pelo usuário
        val dias = Bundle()
        val checkBoxIds = listOf(
            R.id.checkbox_domingo,
            R.id.checkbox_segunda,
            R.id.checkbox_terca,
            R.id.checkbox_quarta,
            R.id.checkbox_quinta,
            R.id.checkbox_sexta,
            R.id.checkbox_sabado
        )

        checkBoxIds.forEach { checkBoxId ->
            val checkBox = findViewById<CheckBox>(checkBoxId)
            checkBox?.let {
                dias.putBoolean(checkBox.text.toString(), checkBox.isChecked)
            }
        }
        return dias
    }

    //botão para passagem de tela
    fun avanc() {
        intent = Intent(this, CriaAlarme::class.java).apply {
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
        }
        startActivity(intent)

}
    }

