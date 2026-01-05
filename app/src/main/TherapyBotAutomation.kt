class MainActivity : AppCompatActivity() {

    private lateinit var btnStartTests: Button
    private lateinit var btnSelectCSV: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvLog: TextView

    private var csvPath: String = ""
    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        checkPermissions()
        setupListeners()
    }

    private fun initViews() {
        btnStartTests = findViewById(R.id.btn_start_tests)
        btnSelectCSV = findViewById(R.id.btn_select_csv)
        tvStatus = findViewById(R.id.tv_status)
        tvLog = findViewById(R.id.tv_log)

        tvStatus.text = "Ready to start"
        btnStartTests.isEnabled = false
    }

    private fun setupListeners() {
        btnSelectCSV.setOnClickListener {
            selectCSVFile()
        }

        btnStartTests.setOnClickListener {
            startAutomationTests()
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val permissionsNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }