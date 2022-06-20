import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

public class ConcreteBank implements Bank {
	
	private ConcurrentHashMap<String, Integer> accounts;
	
	private ConcurrentHashMap<String, Lock> accountLocks;
	
	private ConcurrentHashMap<String, Condition> accountConditions;

	ConcreteBank() {
		this.accounts = new ConcurrentHashMap<String, Integer>();
		this.accountLocks = new ConcurrentHashMap<String, Lock>();
		this.accountConditions = new ConcurrentHashMap<String, Condition>();
	}
	
	@Override
	public boolean addAccount(String accountID, Integer initBalance) {
		Integer result = accounts.putIfAbsent(accountID, initBalance);
		
		// If mapping for the key doesn't exist (account doesn't exist yet), return is null. Otherwise, account already exists
		if (result == null) {
			Lock lock = new ReentrantLock();
			Condition newDeposit = lock.newCondition();
			
			accountLocks.put(accountID, lock);
			accountConditions.put(accountID, newDeposit);
			
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean deposit(String accountID, Integer amount) {
		
		if (!(accounts.containsKey(accountID))) {
			return false;
		}
		
		// Notify all waiting threads
		synchronized (this) {
			accounts.replace(accountID, accounts.get(accountID) + amount);
			notifyAll();
		}
		
		// Already checked for account's existence, so always return true
		return true;
		
		
		/**
		 *  Implementation of having locks on every account, should improve efficiency but didnt
		 */
//		Lock lock = accountLocks.get(accountID);
//		// If no mapping for the accountID, account does not exist and so return null
//		if (lock == null) return false;
//		
//		lock.lock();
//		
//		Integer balance = accounts.get(accountID);
//		// Double check I guess
//		if (balance == null) return false;
//		
//		accounts.replace(accountID, balance + amount);
//		
//		// Notify all waiting threads
//		Condition newDeposit = accountConditions.get(accountID);
//		newDeposit.signalAll();
//		lock.unlock();
//		
//		return true;
	}

	@Override
	public boolean withdraw(String accountID, Integer amount, long timeoutMillis) {
		
		if (!(accounts.containsKey(accountID))) {
			return false;
		}
		
		try {
			
			synchronized (this) {
				// Since withdraw returns false if the balance is still less than amount after waiting,
				// have to use if instead of while here
				Integer balance = accounts.get(accountID);
				
				if (balance < amount) {
					wait(timeoutMillis);
					
					// Notifyall has been called, update the account's balance
					balance = accounts.get(accountID);
				}
				
				if (balance >= amount) {
					// Withdraw amount from account
					accounts.replace(accountID, balance - amount);
					return true;
				}
				
				return false;
			}
			
		}
		catch (InterruptedException ex) {
			ex.printStackTrace();
		}
		
		return false;
		
		
		/**
		 *  Implementation of having locks on every account, should improve throughput but did not somehow
		 */
//		Lock lock = accountLocks.get(accountID);
//		// If no mapping for the accountID, account does not exist and so return null
//		if (lock == null) return false;
//		
//		lock.lock();
//		try {
//			Integer balance = accounts.get(accountID);
//			if (balance == null) return false;
//			
//			if (balance < amount) {
//				// Wait for newDeposit signal for this specific account to be called
//				Condition newDeposit = accountConditions.get(accountID);
////				newDeposit.wait(timeoutMillis);
//				newDeposit.await(timeoutMillis, TimeUnit.MILLISECONDS);
//			}
//			
//			if (balance >= amount) {
//				// Withdraw amount from account
//				accounts.replace(accountID, balance - amount);
//				
//				return true;
//			}
//			
//			return false;
//			
//		} catch (InterruptedException ex) {
//			ex.printStackTrace();
//		} finally {
//			lock.unlock(); // Release the lock
//		}
//		
//		return false;
	}

	@Override
	public boolean transfer(String srcAccount, String dstAccount, Integer amount, long timeoutMillis) {
//		boolean withdrawResult;
//		boolean depositResult;
//		
//		synchronized (srcAccount) {
//			withdrawResult = withdraw(srcAccount, amount, timeoutMillis);
//		}
//		
//		if (withdrawResult) {
//			synchronized (dstAccount) {
//				depositResult = deposit(dstAccount, amount);
//			}
//			if (depositResult) {
//				return true;
//			}
//			else {
//				return false;
//			}
//		}
//		else {
//			return false;
//		}
		
		
		if (withdraw(srcAccount, amount, timeoutMillis)) {
			if (deposit(dstAccount, amount)) {
				return true;
			}
			else
				return false;
		}
		else
			return false;
	}

	@Override
	public Integer getBalance(String accountID) {
		return accounts.get(accountID);
	}

	@Override
	public void doLottery(ArrayList<String> accounts, Miner miner) {
		// TODO Auto-generated method stub
		
		// Multi threaded execution
		ArrayList<Thread> threads = new ArrayList<Thread>();
		
		for (String accID : accounts) {
			Thread t = new Thread(() -> {				
				Integer result;
				
				synchronized (accID) {
					result = miner.mine(accID);
				}
				
				deposit(accID, result);
			});
			
			threads.add(t);
		}
		
		for (Thread t : threads) {
			t.start();
		}
		
		for (Thread t : threads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// Sequential execution
//		for (String accID : accounts) {
//			deposit(accID, miner.mine(accID));
//		}
	}
}
