package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;


import javax.servlet.http.HttpSession;
import com.razorpay.*;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.MyOrderRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.MyOrder;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder; 
	
	@Autowired
	private ContactRepository contactRepository;
	
	@Autowired
	private MyOrderRepository myorderRepository;
	
	@ModelAttribute
	public void addCommonData(Model model,Principal principal) {
		
        String userName=principal.getName();
		
		User user = this.userRepository.getUserByUserName(userName);
		
		System.out.println("USER"+user);
		model.addAttribute("user", user);
	}
	
	@RequestMapping("/index")
	public String dashboard(Model model,Principal principal)
	{
		model.addAttribute("title","User Dashboard");
		
		return "normal/user_dashboard";
	}
	
	//open add form handler
	
	@GetMapping("/add-contact")
	public String openAdContactForm(Model model) {
		
		model.addAttribute("title","Add Contact");
		model.addAttribute("contact", new Contact());
		
		return "normal/add_contact_form";
		
	}
	
	
	//processing add contact form
	
	
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact,
			@RequestParam("profileImage") MultipartFile file ,
			Principal principal,HttpSession session) {
        
		try {
		String name=principal.getName();
		User user = this.userRepository.getUserByUserName(name);
		
		//processing and uploading file
		
		if(file.isEmpty()) {
			
			contact.setImage("contact.png");
			//if file is empty then try our message
		}else {
			//file to folder and update the name to contact
			contact.setImage(file.getOriginalFilename());
           File saveFile = new ClassPathResource("static/img").getFile();
		
           
           Path path= Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
		   
           Files.copy(file.getInputStream(),path,StandardCopyOption.REPLACE_EXISTING);
		
		    System.out.println("IMAGE IS UPLOADED");
		}
		
		contact.setUser(user);
		user.getContacts().add(contact);
		
		
		
		this.userRepository.save(user);
		
		System.out.println("Added to database");
		System.out.println("DATA:"+contact);
          //message success
         session.setAttribute("message", new Message("Your contact is added!!","success"));
		}catch(Exception e) {
			System.out.println("ERROR"+e.getMessage());
			e.printStackTrace();
		    //message error
		    session.setAttribute("message", new Message("Something went wrong!!","danger"));
					
		}
		return "normal/add_contact_form";
	}
	
	
	//show contacts handler
	//per page =5[n]
	//current page=0[page]
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page,Model model,Principal principal) {
		model.addAttribute("title","Show User Contacts");
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		//contacts ki list bhejni h
		Pageable pageable = PageRequest.of(page, 3);
		
		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(),pageable);
		
		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage",page);
		model.addAttribute("totalPages",contacts.getTotalPages());
		
		return "normal/show_contacts";
	}
	
	//showing specific contact detail
	
	
	@RequestMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId,Model model,Principal principal) {
		
		System.out.println(cId);
		
		
		
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);		
		Contact contact =contactOptional.get();
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		if(user.getId()==contact.getUser().getId()) {
		model.addAttribute("contact", contact);
		model.addAttribute("title", contact.getName());
		}
				
		return "normal/contact_detail";
	}
	
	
	//delete contact handler
	
	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId") Integer cId,Model model,HttpSession session,Principal principal){

		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		
		User user=  this.userRepository.getUserByUserName(principal.getName());

		user.getContacts().remove(contact);
		
		this.userRepository.save(user); 
		session.setAttribute("message",new Message("Contact deleted successfully","success"));
		return "redirect:/user/show-contacts/0";
	}
	
	//open update form handler
	
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid,Model model) {
		
		model.addAttribute("title", "Update contact");
		
		Contact contact = this.contactRepository.findById(cid).get();
		
		model.addAttribute("contact", contact);
		return "normal/update-form";
	}
	
	
	// update contact handler
	@RequestMapping(value="/process-update",method=RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file,Model model,HttpSession session,Principal principal) {
		
		try {
			
			//old contact details
      Contact oldContactDetail = this.contactRepository.findById(contact.getcId()).get();
			
			if(!file.isEmpty()) {
				//file work..
				//rewrite
				
				//delete old photo
				  File deleteFile = new ClassPathResource("static/img").getFile();
		          File file1 = new File(deleteFile,oldContactDetail.getImage());
                  file1.delete();		         
				//update new photo
				  File saveFile = new ClassPathResource("static/img").getFile();
							           
		          Path path= Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
		          Files.copy(file.getInputStream(),path,StandardCopyOption.REPLACE_EXISTING);
		  		contact.setImage(file.getOriginalFilename());
		          
			}else {
				contact.setImage(oldContactDetail.getImage());
			}
			
			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);

			session.setAttribute("message",new Message("Your contact is updated...","success"));
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		
		System.out.println("CONTACT:"+contact.getName());
		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	
	//your profile handler
	
	@GetMapping("/profile")
	public String yourProfile(Model m){
		m.addAttribute("title", "Profile Page");
		return "normal/profile";
	}

	//open setting handler
	@GetMapping("/settings")
	public String openSettings()
	{
		return "normal/settings";
	}
	
	
	//change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword,@RequestParam("newPassword") String newPassword,Principal principal,HttpSession session) {
		
		System.out.println("OLD password"+oldPassword);

		System.out.println("NEW password"+newPassword);
		
		
		String userName=principal.getName();
		
		User currentUser = this.userRepository.getUserByUserName(userName);
		
	    if(this.bCryptPasswordEncoder.matches(oldPassword,currentUser.getPassword())) {
	    	
	     //change the password
	    	currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));

	    	this.userRepository.save(currentUser);
	    	session.setAttribute("message",new Message("Your password is successfully changed","success"));
	    }else{
	    	//error
	    	session.setAttribute("message",new Message("Wrong old password","danger"));
	    	return "redirect:/user/settings";
	    	
	    }
		return "redirect:/user/index";
	}
	
	
	//creating order for payment
	
	@PostMapping("/create_order")
	@ResponseBody
	public String createOrder(@RequestBody Map<String,Object> data,Principal principal) throws RazorpayException{
		
		
		System.out.println(data);
		
		int amt = Integer.parseInt(data.get("amount").toString());
		
		var client = new RazorpayClient("rzp_test_zDFgjeFwoe7aQ5","6vQtv57N5GlK1sc6NiejMTFT");
		
		JSONObject ob = new JSONObject();
		ob.put("amount",amt*100);
        ob.put("currency","INR");
		ob.put("receipt","txn_235425");
         
		
		//creating new order
		
		Order order = client.Orders.create(ob);
		System.out.println(order);
		
		
		//save the order in database
		
		MyOrder myOrder = new MyOrder();
		myOrder.setAmount(order.get("amount")+"");
		myOrder.setOrderId(order.get("order_id"));
		myOrder.setPaymentId(null);
		myOrder.setStatus("created");
		myOrder.setUser(this.userRepository.getUserByUserName(principal.getName()));
	    myOrder.setReceipt(order.get("receipt"));
		
	    this.myorderRepository.save(myOrder);
		return order.toString();
		
		
		
	}
	
	@PostMapping("/update_order")
	public ResponseEntity<?> updateOrder(@RequestBody Map<String,Object> data){
		
		MyOrder myOrder = this.myorderRepository.findByOrderId(data.get("order_id").toString());
		
		myOrder.setPaymentId(data.get("payment_id").toString());
		myOrder.setStatus(data.get("status").toString());
		
		this.myorderRepository.save(myOrder);
		
		System.out.println(data);
		return ResponseEntity.ok(Map.of("msg","updated"));
	}
	
}
