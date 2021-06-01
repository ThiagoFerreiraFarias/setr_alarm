package pt.setralarm.presentation.ui

import android.animation.Animator
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import pt.setralarm.MyApplication
import pt.setralarm.R
import pt.setralarm.databinding.FragmentDashboardBinding
import pt.setralarm.presentation.viewmodel.DashboardViewModel
import pt.setralarm.util.AlarmMode

class DashboardFragment : Fragment() {

    private lateinit var bind: FragmentDashboardBinding
    private val mqttService = MyApplication.mqttService
    private val viewModel by lazy { ViewModelProvider(this as ViewModelStoreOwner)[DashboardViewModel::class.java] }
//    private lateinit var navController: NavController


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind = FragmentDashboardBinding.bind(view)
        bind.viewModel = viewModel
        setupUI()
        setupObservers()
        setupButtons()
    }

    private fun setupUI() {
        bind.apply {
            lottieAnim.addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {
                    clFakeSplash.visibility = View.GONE
                    lottieAnim.cancelAnimation()
                }
                override fun onAnimationEnd(animation: Animator?) {}
                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {}

            })

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    private fun setupObservers() {
        bind.viewModel?.apply {

            this.alarmModeType.observe(viewLifecycleOwner, Observer { alarmeMode ->
                bind.apply {
                    setStatus(btnPos1, alarmeMode == AlarmMode.DISARMED)
                    setStatus(btnPos2, alarmeMode == AlarmMode.ARMED_FULL)
                    setStatus(btnPos3, alarmeMode == AlarmMode.ARMED_IN_HOME)
                }
                setSensorsStatus(alarmeMode)
            })

            timerText.observe(viewLifecycleOwner, Observer {
//                bind.tvTimer.text = it
            })

            this.isSystemBlocked.observe(viewLifecycleOwner, Observer {
                it.onHandled {
                    bind.ivLock.setImageResource(if(it.getContent()) R.drawable.ic_lock_closed_24 else R.drawable.ic_lock_open_24)
                }
            })

            instructionMsg.observe(viewLifecycleOwner, Observer {
                it.onHandled {
                    bind.tvFeedMessages.text = it.getContent()
                }
            })

            changeAlarmIcon.observe(viewLifecycleOwner, {
                it.onHandled {
                    bind.ivAlarmInfo.setImageResource(R.drawable.ic_alarm)
                }
            })

            bind.tvPinInput.doOnTextChanged { text, start, before, count ->
                if(count==4) viewModel.submitPin()
            }

        }

        mqttService.apply {
            msgFeed.observe(viewLifecycleOwner, Observer {
                it?.let {
                    bind.tvFeedMessages.text = it
                }
            })

            externalModeStatus = {
                if (it != AlarmMode.UNKNOWN) {
                    bind.tvFeedMessages.text = it.name
                    viewModel.selectedMode(it, true)
                }
            }

            pinValidation = {
                viewModel.setPinValidation(it)
            }

            alarmStatusCheck = { alarm ->
                bind.apply {
                    if (alarm) {
                        ivAlarmInfo.setImageResource(R.drawable.ic_pre_alarm)
                        ivAlarmInfo.visibility = View.VISIBLE
                        viewModel?.startTimer()
                    } else {
                        ivAlarmInfo.visibility = View.GONE
                        viewModel?.cancelTimer()
                    }

                }
            }

            fireCheck= {
                bind.ivAlarmPos1.isEnabled = it
            }
            internalIntrusionCheck= {
                bind.ivAlarmPos2.isEnabled = it
            }
            externalIntrusionCheck= {
                bind.ivAlarmPos3.isEnabled = it
            }

        }
    }

    private fun setSensorsStatus(alarmeMode: AlarmMode) {
        when(alarmeMode){
           AlarmMode.DISARMED -> {
               bind.apply {
                   ivAlarmPos1.apply {
                       isActivated = true
                       isEnabled = false
                   }
                   ivAlarmPos2.apply {
                       isActivated = false
                       isEnabled = false
                   }
                   ivAlarmPos3.apply {
                       isActivated = false
                       isEnabled = false
                   }
               }
           }
           AlarmMode.ARMED_FULL ->{
               bind.apply {
                   ivAlarmPos1.apply {
                       isActivated = true
                       isEnabled = false
                   }
                   ivAlarmPos2.apply {
                       isActivated = true
                       isEnabled = false
                   }
                   ivAlarmPos3.apply {
                       isActivated = true
                       isEnabled = false
                   }
               }
           }
           AlarmMode.ARMED_IN_HOME -> {
               bind.apply {
                   ivAlarmPos1.apply {
                       isActivated = true
                       isEnabled = false
                   }
                   ivAlarmPos2.apply {
                       isActivated = false
                       isEnabled = false
                   }
                   ivAlarmPos3.apply {
                       isActivated = true
                       isEnabled = false
                   }
               }
           }
        }
    }

    private fun setStatus(it: Button, isSelected: Boolean) {
        it.isSelected = isSelected
        it.setTextColor(getColorByStatus(it.isSelected))

    }

    private fun getColorByStatus(isSelected: Boolean): Int {
        return if (isSelected) Color.GREEN else Color.WHITE
    }

    private fun setupButtons() {
        bind.apply {
            viewModel?.let { vm->
                key1.setOnClickListener { vm.getKeyAction(key1.text.toString()) }
                key2.setOnClickListener { vm.getKeyAction(key2.text.toString()) }
                key3.setOnClickListener { vm.getKeyAction(key3.text.toString()) }
                key4.setOnClickListener { vm.getKeyAction(key4.text.toString()) }
                key5.setOnClickListener { vm.getKeyAction(key5.text.toString()) }
                key6.setOnClickListener { vm.getKeyAction(key6.text.toString()) }
                key7.setOnClickListener { vm.getKeyAction(key7.text.toString()) }
                key8.setOnClickListener { vm.getKeyAction(key8.text.toString()) }
                key9.setOnClickListener { vm.getKeyAction(key9.text.toString()) }
                key0.setOnClickListener { vm.getKeyAction(key0.text.toString()) }
                keyCancel.setOnClickListener { vm.getKeyAction(keyCancel.text.toString()) }
                keyConfirm.setOnClickListener { vm.getKeyAction(keyConfirm.text.toString()) }
            }

            btnPanicButton.setOnClickListener {
                viewModel?.onPanicBtnClick()
            }

            clBtnChangePin.setOnClickListener {
                viewModel?.onRefreshPinClick()
            }

            btnResetAlarm.setOnClickListener {
                viewModel?.resetAlarm()
            }
        }
    }

}
