package com.example.coinapp.ui.coinDetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.coinapp.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CoinDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val coinRepository by lazy { CoinRepository.getInstance(application) }
    private val transactionRepository by lazy { TransactionRepository.getInstance(application) }

    private val _coin = MutableLiveData<Coin>()

    private val _transactions = MutableLiveData<List<Transaction>>().apply {
        value = emptyList()
    }

    val transactions: LiveData<List<Transaction>>
        get() = _transactions

    val coin: LiveData<Coin>
        get() = _coin

    fun unwatchCoin() {
        viewModelScope.launch(Dispatchers.IO) {
            coin.value?.let { coinRepository.deleteCoin(it) }
        }
    }

    /**
     * Updates [_transactions] LiveData field with sorted transactions from database
     */
    fun getTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _transactions.postValue(transactionRepository.getAllFromDatabase())
            } catch (e: Exception) {

            }
        }
    }

    /**
     * Sets new value to [_coin] LiveData field
     */
    fun setCoin(newCoin: Coin) {
        _coin.value = newCoin
    }

    /**
     * Returns List of transactions with transaction type of [type]
     */
    private fun getTransactionsByType(type: TransactionType): List<Transaction> {
        return transactions.value
            ?.filter { transaction -> transaction.type == type }
            ?: emptyList()
    }

    /**
     * Returns sum of all [FeeType.DOLLAR] fees
     */
    private fun getFeesSum(type: FeeType): Double {
        return transactions.value
            ?.filter { transaction -> transaction.feeType == type }
            ?.map { transaction -> transaction.fee }
            ?.sum() ?: 0.0
    }

    /**
     * Returns dollar value of all transactions with [FeeType.COIN]
     */
    private fun getCoinFeesInDollars(): Double {
        return transactions.value
            ?.filter { transaction -> transaction.feeType == FeeType.COIN }
            ?.map { transaction -> transaction.fee * (transaction.cost / transaction.amount) }
            ?.sum() ?: 0.0
    }

    /**
     * Return sum of coin amount of all transactions from [pickedTransactions] list
     */
    private fun getSumOfTransactionsAmount(pickedTransactions: List<Transaction>): Double {
        return pickedTransactions
            .map { transaction -> transaction.amount }
            .sum()
    }

    /**
     * Returns sum of cost of all transactions from [pickedTransactions] List
     */
    private fun getSumOfTransactionCost(pickedTransactions: List<Transaction>): Double {
        return pickedTransactions
            .map { transaction -> transaction.cost }
            .sum()
    }

    /**
     * Counts total amount of held coin
     * Formula: Sum of all buy transactions - Sum of Sell transactions - Sum of Send_Out transactions - Coin Fees
     */
    fun countHoldings(): Double {
        val buyTransactions = getSumOfTransactionsAmount(getTransactionsByType(TransactionType.BUY))

        val sellTransactions =
            getSumOfTransactionsAmount(getTransactionsByType(TransactionType.SELL))

        val sendInTransactions =
            getSumOfTransactionsAmount(getTransactionsByType(TransactionType.RECEIVE))

        val sendOutTransactions =
            getSumOfTransactionsAmount(getTransactionsByType(TransactionType.SEND))

        return buyTransactions + sendInTransactions - sellTransactions - sendOutTransactions -
                getFeesSum(FeeType.COIN)
    }

    /**
     * Counts current value of held coins
     * Formula: Sum of all holdings * current price of coin
     */
    fun countHoldingsValue(): Double {
        return countHoldings() * (coin.value?.price ?: 0.0)
    }

    /**
     * Counts total amount of money spent on the currently held coins
     * Formula: Sum of cost of all Buy Transactions - Sum of all Sell transactions + Sum of All Fees
     */
    fun countTotalCost(): Double {
        val buyTransactions = getSumOfTransactionCost(getTransactionsByType(TransactionType.BUY))
        val sellTransactions = getSumOfTransactionCost(getTransactionsByType(TransactionType.SELL))

        return buyTransactions - sellTransactions + getFeesSum(FeeType.DOLLAR) + getCoinFeesInDollars()
    }

    /**
     * Counts average price of all transactions
     * Formula: Sum of transactions cost / Sum of current holdings
     */
    fun countAverageTransactionCost(): Double {
        return countTotalCost() / countHoldings()
    }

    /**
     * Counts difference between current value of coins and total cost of coins
     * Formula: Current holdings value - Total cost
     */
    fun countProfitOrLoss(): Double {
        return countHoldingsValue() - countTotalCost()
    }

    /**
     * Counts the percentage difference between value of current holdings and total cost
     * Formula: Current holdings value / Total Cost - 1
     */
    fun countPercentageChange(): Double {
        return (countHoldingsValue() / countTotalCost()) - 1
    }
}