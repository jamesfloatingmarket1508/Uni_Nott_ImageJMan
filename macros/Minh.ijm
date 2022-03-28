

macro "Minh" {

  print("remove last element");
  a = newArray(1,2,3,4,5);
  a = Array.slice(a, 0, a.length-1);
  Array.print(a);


}