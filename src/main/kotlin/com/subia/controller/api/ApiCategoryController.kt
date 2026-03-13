package com.subia.controller.api

import com.subia.dto.api.ApiResponse
import com.subia.dto.api.CategoryDto
import com.subia.dto.api.CategoryRequestDto
import com.subia.model.Category
import com.subia.service.CategoryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/categories")
class ApiCategoryController(private val categoryService: CategoryService) {

    @GetMapping
    fun listAll(): ApiResponse<List<CategoryDto>> =
        ApiResponse(data = categoryService.findAll().map { it.toDto() })

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ApiResponse<CategoryDto> =
        ApiResponse(data = categoryService.findById(id).toDto())

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: CategoryRequestDto): ApiResponse<CategoryDto> =
        ApiResponse(data = categoryService.save(Category(name = req.name, color = req.color, icon = req.icon)).toDto())

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody req: CategoryRequestDto): ApiResponse<CategoryDto> {
        // Verify it exists before updating (throws NoSuchElementException if not found)
        categoryService.findById(id)
        return ApiResponse(data = categoryService.save(Category(id = id, name = req.name, color = req.color, icon = req.icon)).toDto())
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) { categoryService.delete(id) }

    private fun Category.toDto() = CategoryDto(id = id, name = name, color = color, icon = icon)
}