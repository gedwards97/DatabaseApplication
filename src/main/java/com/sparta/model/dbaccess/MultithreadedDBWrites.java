package com.sparta.model.dbaccess;

import com.sparta.model.employee.Employee;
import com.sparta.model.util.PrintTimingData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static com.sparta.model.util.Constants.LOGGER;

public class MultithreadedDBWrites {
    private static final int poolSize = 128;

    public static void writeNonDuplicatesOnly(Set<Employee> toWrite){
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        BlockingQueue<Employee> employeeBlockingQueue = new ArrayBlockingQueue<>(toWrite.size());
        employeeBlockingQueue.addAll(toWrite);
        List<Future<?>> areDone = new ArrayList<>(poolSize);
        for (int i = 0 ; i < poolSize ; i++){
            areDone.add(pool.submit(new DBWriter(employeeBlockingQueue)));
        }
        pool.shutdown();
        for (Future<?> future: areDone){
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}

class DBWriter implements Runnable{
    private final BlockingQueue<Employee> chunk;
    public DBWriter(BlockingQueue<Employee> chunk){
        this.chunk = chunk;
    }

    @Override
    public void run() {
        EmployeeDao employeeDao = new EmployeeDaoImpl();
        int masterCount = 0;
        int batchCount = 0;
        int numberOfBatches = 0;
        int batchSize = 256;
        Employee employee;
        List<Employee> employeeBatch = new ArrayList<>(batchSize);
        while((employee = chunk.poll()) != null){
            // employeeDao.insertEmployee(employee);
            employeeBatch.add(employee);
            masterCount+=1;
            batchCount+=1;
            if (employeeBatch.size() == batchSize){
                employeeDao.insertEmployeeBatch(employeeBatch);
                batchCount=0;
                numberOfBatches+=1;
                employeeBatch.clear();
            }
            if (masterCount % batchSize == 0){
                LOGGER.info("Thread " + Thread.currentThread().getId() + " has inserted " + numberOfBatches + " batches (" + masterCount +") records");
            }
        }
        if (!employeeBatch.isEmpty()){
            employeeDao.insertEmployeeBatch(employeeBatch);
        }
    }
}