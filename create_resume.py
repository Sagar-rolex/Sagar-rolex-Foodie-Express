from docx import Document
from docx.shared import Inches, Pt
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import nsdecls
from docx.oxml import parse_xml
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.style import WD_STYLE_TYPE

doc = Document()

# Page setup - US Letter
section = doc.sections[0]
section.page_width = Inches(8.5)
section.page_height = Inches(11)

# Set margins
section.top_margin = Inches(0.75)
section.bottom_margin = Inches(0.75)
section.left_margin = Inches(0.75)
section.right_margin = Inches(0.75)

# Name
name_para = doc.add_paragraph()
name_run = name_para.add_run('SAHIL TANAJI SALUNKHE')
name_run.bold = True
name_run.font.size = Pt(28)
name_para.alignment = WD_ALIGN_PARAGRAPH.CENTER

# Subtitle
subtitle_para = doc.add_paragraph()
subtitle_run = subtitle_para.add_run('Aspiring Software Developer | MCA Candidate | Navi Mumbai, Maharashtra, India')
subtitle_run.font.size = Pt(22)
subtitle_para.alignment = WD_ALIGN_PARAGRAPH.CENTER

# Contact
contact_para = doc.add_paragraph()
contact_run = contact_para.add_run('+91 XXXXXXXXXX | sahil.salunkhe@email.com | linkedin.com/in/sahil-salunkhe | github.com/sahil-salunkhe')
contact_run.font.size = Pt(20)
contact_para.alignment = WD_ALIGN_PARAGRAPH.CENTER

# Add bottom border to contact line
border_para = doc.add_paragraph()
border_run = border_para.add_run()
border_run.font.size = Pt(1)
border_para.add_run('_' * 80)
border_para.alignment = WD_ALIGN_PARAGRAPH.CENTER
border_para.space_after = Pt(100)

# Professional Summary
summary_heading = doc.add_paragraph()
summary_run = summary_heading.add_run('PROFESSIONAL SUMMARY')
summary_run.bold = True
summary_run.font.size = Pt(24)
summary_run.font.color.rgb = None  # Keep default

summary_text = doc.add_paragraph()
summary_run = summary_text.add_run('Highly motivated and detail-oriented technology student with a BCA degree, currently pursuing an MCA at SIES College of Management Studies. Combines solid academic foundation in computer science with practical programming abilities. Passionate about problem-solving, web development, and building scalable applications. Eager to contribute to innovative software development projects while delivering tangible results in a collaborative team environment.')
summary_run.font.size = Pt(22)

doc.add_paragraph()

# Technical Skills
skills_heading = doc.add_paragraph()
skills_run = skills_heading.add_run('TECHNICAL SKILLS')
skills_run.bold = True
skills_run.font.size = Pt(24)
skills_run.font.color.rgb = None

# Skills list
skills_data = [
    ('Programming Languages', 'Java, JavaScript, SQL'),
    ('Frontend', 'React.js, HTML5, CSS3, Responsive Design'),
    ('Backend', 'Node.js, Express.js'),
    ('Databases', 'MySQL, MongoDB'),
    ('Tools & Platforms', 'Git, VS Code, Postman, Firebase')
]

for category, skills in skills_data:
    skill_para = doc.add_paragraph()
    category_run = skill_para.add_run(f'{category}: ')
    category_run.bold = True
    category_run.font.size = Pt(22)
    skill_run = skill_para.add_run(skills)
    skill_run.font.size = Pt(22)

doc.add_paragraph()

# Education
edu_heading = doc.add_paragraph()
edu_run = edu_heading.add_run('EDUCATION')
edu_run.bold = True
edu_run.font.size = Pt(24)
edu_run.font.color.rgb = None

# Education Table
table = doc.add_table(rows=3, cols=4)
table.style = 'Table Grid'
table.alignment = WD_TABLE_ALIGNMENT.CENTER

# Header row
header_cells = table.rows[0].cells
headers = ['Course', 'Year', 'Institution', 'Result']
for i, header in enumerate(headers):
    header_cells[i].text = header
    header_cells[i].paragraphs[0].runs[0].bold = True
    header_cells[i].paragraphs[0].runs[0].font.size = Pt(20)
    header_cells[i].paragraphs[0].runs[0].font.color.rgb = None  # Dark blue header
    # Set header background color
    shading_elm = parse_xml(r'<w:shd {} w:fill="1F3864"/>'.format(nsdecls('w')))
    header_cells[i]._tc.get_or_add_tcPr().append(shading_elm)
    header_cells[i].paragraphs[0].alignment = WD_ALIGN_PARAGRAPH.CENTER
    header_cells[i].vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER

# Data rows
data_rows = [
    ('Master of Computer Applications', '2025 - Present', 'SIES College of Management Studies, Nerul, Navi Mumbai', 'Pursuing'),
    ('Bachelor of Computer Applications', '2022 - 2025', 'SIES College of Management Studies, Nerul, Navi Mumbai', '7.5+ CGPA')
]

for row_idx, row_data in enumerate(data_rows):
    row = table.rows[row_idx + 1].cells
    for col_idx, text in enumerate(row_data):
        row[col_idx].text = text
        row[col_idx].paragraphs[0].runs[0].font.size = Pt(20)
        if col_idx > 0:
            row[col_idx].paragraphs[0].alignment = WD_ALIGN_PARAGRAPH.CENTER

doc.add_paragraph()

# Projects
projects_heading = doc.add_paragraph()
projects_run = projects_heading.add_run('PROJECTS')
projects_run.bold = True
projects_run.font.size = Pt(24)
projects_run.font.color.rgb = None

# Project 1
project1_title = doc.add_paragraph()
p1_run = project1_title.add_run('AI Resume Builder')
p1_run.bold = True
p1_run.font.size = Pt(22)
p1_date = project1_title.add_run(' (Nov 2025 - Feb 2026)')
p1_date.font.size = Pt(22)

doc.add_paragraph('Developed an AI-powered resume builder that streamlines resume creation for job seekers by generating professional summaries, impactful work bullet points, and optimized skill lists based on user-provided career history or job descriptions.', style='List Bullet')
doc.add_paragraph('Incorporated industry-specific keywords to ensure generated resumes are ATS-friendly, increasing likelihood of passing automated screening processes.', style='List Bullet')
doc.add_paragraph('Implemented responsive design templates and PDF export functionality, allowing users to customize and share polished resumes with potential employers.', style='List Bullet')

# Project 2
doc.add_paragraph()
project2_title = doc.add_paragraph()
p2_run = project2_title.add_run('Online Library Management System')
p2_run.bold = True
p2_run.font.size = Pt(22)
p2_date = project2_title.add_run(' (Nov 2024 - Feb 2025)')
p2_date.font.size = Pt(22)

doc.add_paragraph('Developed a comprehensive web-based system to digitize and automate library operations, addressing the need for efficient library management.', style='List Bullet')
doc.add_paragraph('Implemented features including book catalog management, member registration, issue/return tracking, and fine calculation system with database integration.', style='List Bullet')
doc.add_paragraph('Built with Node.js backend, React frontend, and MongoDB database, providing a user-friendly interface for librarians and members.', style='List Bullet')

doc.add_paragraph()

# Work Experience
work_heading = doc.add_paragraph()
work_run = work_heading.add_run('WORK EXPERIENCE')
work_run.bold = True
work_run.font.size = Pt(24)
work_run.font.color.rgb = None

work_title = doc.add_paragraph()
w_run = work_title.add_run('Internship - Web Development')
w_run.bold = True
w_run.font.size = Pt(22)
w_date = work_title.add_run(' (Jun 2024 - Aug 2024)')
w_date.font.size = Pt(22)

doc.add_paragraph('Collaborated with development team to build responsive web applications using React.js and Node.js, following industry best practices and coding standards.', style='List Bullet')
doc.add_paragraph('Participated in code reviews, debugging, and optimization of web applications; gained practical experience in version control using Git and agile development processes.', style='List Bullet')

doc.add_paragraph()

# Certifications
cert_heading = doc.add_paragraph()
cert_run = cert_heading.add_run('CERTIFICATIONS & ACHIEVEMENTS')
cert_run.bold = True
cert_run.font.size = Pt(24)
cert_run.font.color.rgb = None

certifications = [
    'Full Stack Web Development Certification - Udemy',
    'JavaScript Mastery Course - Completion Certificate',
    'Problem-Solving Excellence Award - College'
]

for cert in certifications:
    doc.add_paragraph(f'• {cert}', style='List Bullet')

doc.add_paragraph()

# Additional Skills
add_heading = doc.add_paragraph()
add_run = add_heading.add_run('ADDITIONAL SKILLS')
add_run.bold = True
add_run.font.size = Pt(24)
add_run.font.color.rgb = None

doc.add_paragraph('Communication Skills, Team Collaboration, Problem-Solving, Quick Learner, Time Management, Interpersonal Skills, Attention to Detail')

# Save the document
doc.save('E:\\foodie-express\\Sahil_Tanaji_Salunkhe_Resume.docx')
print('Resume created successfully!')