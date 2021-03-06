package simpledb;
import java.io.*;

public class Lab2Main {

    public static void main(String[] argv) {
        // construct a 3-column table schema
        Type types[] = new Type[]{ Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE };
        String names[] = new String[]{ "field0", "field1", "field2" };
        TupleDesc descriptor = new TupleDesc(types, names);

        // create the table, associate it with some_data_file.dat
        // and tell the catalog about the schema of this table.
        HeapFile table1 = new HeapFile(new File("some_data_file.dat"), descriptor);
        Database.getCatalog().addTable(table1, "test");

        // construct the query: we use a simple SeqScan, which spoonfeeds
        // tuples via its iterator.
        TransactionId tid = new TransactionId();
        SeqScan f = new SeqScan(tid, table1.getId());
       
        try {
            // and run it
            f.open();
            while (f.hasNext()) {
                Tuple tup = f.next();
                if (((IntField) tup.getField(1)).getValue() < 3) {
                    System.out.print("Update tuple: " + tup.toString());
                    System.out.println("to be: ");
                    table1.deleteTuple(tid, tup);
                    tup.setField(1, new IntField(3));
                    table1.insertTuple(tid, tup);
                    System.out.println(tup.toString());
                }              
            }
            Tuple t = new Tuple(descriptor);
            t.setField(0, new IntField(99));
            t.setField(1, new IntField(99));
            t.setField(2, new IntField(99));
            System.out.println("Insert Tuple: " + t.toString());
            table1.insertTuple(tid, t);
            
            f.rewind();
            System.out.println("The table now contains the following records: ");
            while (f.hasNext()) {
                Tuple tup2 = f.next();
                System.out.print("Tuple: ");
                System.out.println(tup2);
            }
            
            f.close();
            Database.getBufferPool().transactionComplete(tid);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println ("Exception : " + e);
        }
    }

}