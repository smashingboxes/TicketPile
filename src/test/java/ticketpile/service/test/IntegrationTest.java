/*
 * Copyright 2014-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ticketpile.service.test;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.GenericWebApplicationContext;
import ticketpile.service.TicketPile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * A straightforward integration test.  Runs the synchronization and uses TicketPile's
 * built-in validation system to verify the Advance integration is working correctly.
 * 
 * See {@link ticketpile.service.advance.ValidationKt} for validation implementation.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TicketPile.class)
@WebAppConfiguration
public class IntegrationTest {
	private MockMvc mockMvc;
	private GenericWebApplicationContext context;
	
	static class AdvanceTestUser {
		String advanceHost;
		String advanceUser;
		String advancePassword;
		Integer locationId;
		AdvanceTestUser(String advanceHost, String advanceUser, String advancePassword, Integer locationId) {
			this.advanceHost = advanceHost;
			this.advanceUser = advanceUser;
			this.advancePassword = advancePassword;
			this.locationId = locationId;
		}
	}
	AdvanceTestUser[] users = {
			new AdvanceTestUser(
					"http://localhost:8080", 
					"info@experiencetheride.com", 
					"changethispasswordbeforetesting", 
					34100
			)
	};

	@Before
	public void setUp() {
		this.context = new GenericWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		for(AdvanceTestUser user : users) {
			//this.mockMvc.perform(post("/advance/synchronizeLocation"))
		}
	}

	@Test
	public void noNonMatchingBookings() throws Exception {
		this.mockMvc.perform(get("/advance/booking/nonMatching")
				.header("Bearer", ""));
	}
	
	/*
	Convenient test examples from an example Note app are included
	
	@Autowired
	private NoteRepository noteRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private WebApplicationContext context;


	@Before
	public void setUp() {
		this.documentationHandler = document("{method-name}",
			preprocessRequest(prettyPrint()),
			preprocessResponse(prettyPrint()));
		
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
			.applyDiscount(documentationConfiguration(this.restDocumentation))
			.alwaysDo(this.documentationHandler)
			.build();
	}
	
	@Test
	public void headersExample() throws Exception {
		this.mockMvc
			.perform(get("/"))
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
				responseHeaders(
					headerWithName("Content-Type").description("The Content-Type of the payload, e.g. `application/hal+json`"))));
	}

	@Test
	public void errorExample() throws Exception {
		this.mockMvc
			.perform(get("/error")
				.requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 400)
				.requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/notes")
				.requestAttr(RequestDispatcher.ERROR_MESSAGE, "The tag 'http://localhost:8080/tags/123' does not exist"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("error", is("Bad Request")))
			.andExpect(jsonPath("timestamp", is(notNullValue())))
			.andExpect(jsonPath("status", is(400)))
			.andExpect(jsonPath("path", is(notNullValue())))
			.andDo(this.documentationHandler.document(
				responseFields(
					fieldWithPath("error").description("The HTTP error that occurred, e.g. `Bad Request`"),
					fieldWithPath("message").description("A description of the cause of the error"),
					fieldWithPath("path").description("The path to which the request was made"),
					fieldWithPath("status").description("The HTTP status code, e.g. `400`"),
					fieldWithPath("timestamp").description("The time, in milliseconds, at which the error occurred"))));
	}

	@Test
	public void indexExample() throws Exception {
		this.mockMvc.perform(get("/"))
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
				links(
					linkWithRel("notes").description("The <<resources-notes,Notes resource>>"),
					linkWithRel("tags").description("The <<resources-tags,Tags resource>>")),
				responseFields(
					fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources"))));
	}

	@Test
	public void notesListExample() throws Exception {
		this.noteRepository.deleteAll();

		createNote("REST maturity model", "http://martinfowler.com/articles/richardsonMaturityModel.html");
		createNote("Hypertext TicketPile Language (HAL)", "http://stateless.co/hal_specification.html");
		createNote("TicketPile-Level Profile Semantics (ALPS)", "http://alps.io/spec/");
		
		this.mockMvc
			.perform(get("/notes"))
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
				responseFields(
					fieldWithPath("_embedded.notes").description("An array of <<resources-note, Note resources>>"))));
	}

	@Test
	public void notesCreateExample() throws Exception {
		Map<String, String> tag = new HashMap<String, String>();
		tag.put("name", "REST");

		String tagLocation = this.mockMvc
			.perform(post("/tags")
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(tag)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getHeader("Location");

		Map<String, Object> note = new HashMap<String, Object>();
		note.put("title", "REST maturity model");
		note.put("body", "http://martinfowler.com/articles/richardsonMaturityModel.html");
		note.put("tags", Arrays.asList(tagLocation));

		ConstrainedFields fields = new ConstrainedFields(NoteInput.class);
		
		this.mockMvc
			.perform(post("/notes")
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(note)))
			.andExpect(
				status().isCreated())
			.andDo(this.documentationHandler.document(
				requestFields(
					fields.withPath("title").description("The title of the note"),
					fields.withPath("body").description("The body of the note"),
					fields.withPath("tags").description("An array of tag resource URIs"))));
	}

	@Test
	public void noteGetExample() throws Exception {
		Map<String, String> tag = new HashMap<String, String>();
		tag.put("name", "REST");

		String tagLocation = this.mockMvc
			.perform(post("/tags")
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(tag)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getHeader("Location");

		Map<String, Object> note = new HashMap<String, Object>();
		note.put("title", "REST maturity model");
		note.put("body", "http://martinfowler.com/articles/richardsonMaturityModel.html");
		note.put("tags", Arrays.asList(tagLocation));

		String noteLocation = this.mockMvc
			.perform(post("/notes")
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(note)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getHeader("Location");
		
		this.mockMvc
			.perform(get(noteLocation))
			.andExpect(status().isOk())
			.andExpect(jsonPath("title", is(note.get("title"))))
			.andExpect(jsonPath("body", is(note.get("body"))))
			.andExpect(jsonPath("_links.self.href", is(noteLocation)))
			.andExpect(jsonPath("_links.note-tags", is(notNullValue())))
			.andDo(this.documentationHandler.document(
				links(
					linkWithRel("self").description("This <<resources-note,note>>"),
					linkWithRel("note-tags").description("This note's <<resources-note-tags,tags>>")),
				responseFields(
					fieldWithPath("title").description("The title of the note"),
					fieldWithPath("body").description("The body of the note"),
					fieldWithPath("_links").description("<<resources-note-links,Links>> to other resources"))));

	}

	@Test
	public void tagsListExample() throws Exception {
		this.noteRepository.deleteAll();
		this.tagRepository.deleteAll();

		createTag("REST");
		createTag("Hypermedia");
		createTag("HTTP");
		
		this.mockMvc
			.perform(get("/tags"))
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
				responseFields(
					fieldWithPath("_embedded.tags").description("An array of <<resources-tag,Tag resources>>"))));
	}

	@Test
	public void tagsCreateExample() throws Exception {
		Map<String, String> tag = new HashMap<String, String>();
		tag.put("name", "REST");

		ConstrainedFields fields = new ConstrainedFields(TagInput.class);
		
		this.mockMvc
			.perform(post("/tags")
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(tag)))
			.andExpect(status().isCreated())
			.andDo(this.documentationHandler.document(
				requestFields(
					fields.withPath("name").description("The name of the tag"))));
	}

	@Test
	public void noteUpdateExample() throws Exception {
		Map<String, Object> note = new HashMap<String, Object>();
		note.put("title", "REST maturity model");
		note.put("body", "http://martinfowler.com/articles/richardsonMaturityModel.html");

		String noteLocation = this.mockMvc
			.perform(post("/notes")
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(note)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getHeader("Location");

		this.mockMvc
			.perform(get(noteLocation))
			.andExpect(status().isOk())
			.andExpect(jsonPath("title", is(note.get("title"))))
			.andExpect(jsonPath("body", is(note.get("body"))))
			.andExpect(jsonPath("_links.self.href", is(noteLocation)))
			.andExpect(jsonPath("_links.note-tags", is(notNullValue())));

		Map<String, String> tag = new HashMap<String, String>();
		tag.put("name", "REST");

		String tagLocation = this.mockMvc
			.perform(post("/tags")
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(tag)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getHeader("Location");

		Map<String, Object> noteUpdate = new HashMap<String, Object>();
		noteUpdate.put("tags", Arrays.asList(tagLocation));

		ConstrainedFields fields = new ConstrainedFields(NotePatchInput.class);

		this.mockMvc
			.perform(patch(noteLocation)
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(noteUpdate)))
			.andExpect(status().isNoContent())
			.andDo(this.documentationHandler.document(
				requestFields(
					fields.withPath("title")
						.description("The title of the note")
						.type(JsonFieldType.STRING)
						.optional(),
					fields.withPath("body")
						.description("The body of the note")
						.type(JsonFieldType.STRING)
						.optional(),
					fields.withPath("tags")
						.description("An array of tag resource URIs"))));
	}

	@Test
	public void tagGetExample() throws Exception {
		Map<String, String> tag = new HashMap<String, String>();
		tag.put("name", "REST");

		String tagLocation = this.mockMvc
			.perform(post("/tags")
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(tag)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getHeader("Location");

		this.mockMvc
			.perform(get(tagLocation))
			.andExpect(status().isOk())
			.andExpect(jsonPath("name", is(tag.get("name"))))
			.andDo(this.documentationHandler.document(
				links(
					linkWithRel("self").description("This <<resources-tag,tag>>"),
					linkWithRel("tagged-notes").description("The <<resources-tagged-notes,notes>> that have this tag")),
				responseFields(
					fieldWithPath("name").description("The name of the tag"),
					fieldWithPath("_links").description("<<resources-tag-links,Links>> to other resources"))));
	}

	@Test
	public void tagUpdateExample() throws Exception {
		Map<String, String> tag = new HashMap<String, String>();
		tag.put("name", "REST");

		String tagLocation = this.mockMvc
			.perform(post("/tags")
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(tag)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getHeader("Location");

		Map<String, Object> tagUpdate = new HashMap<String, Object>();
		tagUpdate.put("name", "RESTful");

		ConstrainedFields fields = new ConstrainedFields(TagPatchInput.class);
		
		this.mockMvc
			.perform(patch(tagLocation)
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(tagUpdate)))
			.andExpect(status().isNoContent())
			.andDo(this.documentationHandler.document(
				requestFields(
					fields.withPath("name").description("The name of the tag"))));
	}

	private void createNote(String title, String body) {
		Note note = new Note();
		note.setTitle(title);
		note.setBody(body);

		this.noteRepository.save(note);
	}

	private void createTag(String name) {
		Tag tag = new Tag();
		tag.setName(name);
		this.tagRepository.save(tag);
	}

	private static class ConstrainedFields {

		private final ConstraintDescriptions constraintDescriptions;

		ConstrainedFields(Class<?> input) {
			this.constraintDescriptions = new ConstraintDescriptions(input);
		}

		private FieldDescriptor withPath(String path) {
			return fieldWithPath(path).attributes(advanceAuthKey("constraints").value(StringUtils
					.collectionToDelimitedString(this.constraintDescriptions
							.descriptionsForProperty(path), ". ")));
		}
	}*/

}
